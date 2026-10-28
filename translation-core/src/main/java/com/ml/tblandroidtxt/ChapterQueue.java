package com.ml.tblandroidtxt;

import java.util.List;

/**
 * The per-chapter loop of a run (one RAW after another, each with its own Glossary/Pronoun binding), shared by the phone service and
 * the PC. The loop body is the one that lived in the phone's TranslatorService; preparing the inputs and starting one job stay with
 * the caller, passed in as a {@link Step}.
 */
final class ChapterQueue {
    /** The run stopped because too many chapters in a row failed their validation; the message is the sentence shown to the person. */
    static class QueueStoppedException extends IllegalStateException {
        QueueStoppedException(String message) { super(message); }
    }

    /** One RAW of the run. {@code binding} is null outside chapter mode; {@code resumeJobId} continues an unfinished job. */
    static final class Task {
        final String displayName;
        final ChapterBinding binding;
        final Long resumeJobId;
        /** Whatever the caller's {@link Step} needs to start this input (the phone keeps its prepared input here). */
        final Object payload;

        Task(String displayName, ChapterBinding binding, Long resumeJobId, Object payload) {
            this.displayName = displayName;
            this.binding = binding;
            this.resumeJobId = resumeJobId;
            this.payload = payload;
        }
    }

    /** Starts a brand-new job for one task with the settings the queue chose for it. */
    interface Step {
        void startNew(Task input, AppSettings s, int fileNo, int fileTotal, String label) throws Exception;
    }

    private ChapterQueue() {}

    /**
     * Runs every task in order. Returns false when the run was cancelled before the next task could start (the caller finishes the
     * cancellation); true when the queue ended and the closing state and line have been published. A chapter failure that belongs to
     * one chapter moves on to the next; too many in a row throws {@link QueueStoppedException}.
     */
    static boolean run(TranslationRunner runner, TranslationRepositoryCore repo, List<Task> tasks, AppSettings s, Step step) throws Exception {
        for(int i=0;i<tasks.size();i++){
            runner.waitIfPaused();if(runner.isCancelled()){return false;}
            Task input=tasks.get(i);
            ChapterRunOutcome outcome=runner.getChapterOutcome();
            final Long resumeId=input.resumeJobId;
            final int fileNo=i+1,fileTotal=tasks.size();
            if(outcome==null){
                if(resumeId!=null)runner.continueExistingJob(resumeId,fileNo,fileTotal,input.displayName);
                else step.startNew(input,s,fileNo,fileTotal,input.displayName);
                continue;
            }
            ChapterBinding binding=input.binding;
            runner.setSetIndex(i+1);
            String label="Chương "+(binding.chapterNo>=0?binding.chapterNo:i+1)+" ("+(i+1)+"/"+tasks.size()+") · "+input.displayName;
            try{
                if(resumeId!=null)runner.continueExistingJob(resumeId,fileNo,fileTotal,label);
                else step.startNew(input,binding.applyTo(s),fileNo,fileTotal,label);
                if(!runner.isCancelled())outcome.done(input.displayName);
            }catch(Exception e){
                if(runner.isCancelled())throw e;
                String here="Chương "+(binding.chapterNo>=0?binding.chapterNo:i+1);
                outcome.failed(input.displayName,AppValidator.readableError(e),here);
                if(ChapterRunOutcome.continuesQueue(e)){
                    if(runner.getActiveJobId()>0)repo.touchJob(runner.getActiveJobId(),"error");
                    if(outcome.shouldStopQueue())throw new QueueStoppedException(outcome.stopSentence());
                    String next=i+1<tasks.size()?"chuyển chương "+(tasks.get(i+1).binding.chapterNo>=0?tasks.get(i+1).binding.chapterNo:i+2):"hết tập";
                    runner.publish("running",runner.percent(i+1,tasks.size()),here+" lỗi, "+next+": "+AppValidator.readableError(e));
                    continue;
                }
                throw e;
            }
        }
        ChapterRunOutcome finished=runner.getChapterOutcome();
        runner.setState(TranslationJobState.State.COMPLETED,"Completed");runner.publish("done",100,finished==null?"Prepared batch translation completed":finished.summary());
        return true;
    }
}

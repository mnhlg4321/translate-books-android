package com.ml.tblandroidtxt;
import java.util.ArrayList;
import java.util.List;

/** Deterministic fake-provider harness for release reliability scenarios. */
public final class ReliabilityScenarioRunner {
    public interface Provider { Reply request(int chunkIndex,int attempt) throws Exception; }
    public static class Reply {public final String text,finish;public final int tokens;public final double cost;public Reply(String t,String f,int n,double c){text=t;finish=f;tokens=n;cost=c;}}
    public static class State {public final List<Entry> chunks=new ArrayList<>();public double actualCost;public boolean paused;public String pauseReason="";public String output(){StringBuilder b=new StringBuilder();for(Entry e:chunks)if("COMPLETED".equals(e.status))b.append(e.accepted);return b.toString();}}
    public static class Entry {public int index,attempts;public String source,accepted="",candidate="",status="PENDING",error="";Entry(int i,String s){index=i;source=s;}}
    public static State create(List<String>sources){State s=new State();for(int i=0;i<sources.size();i++)s.chunks.add(new Entry(i,sources.get(i)));return s;}
    public static void run(State state,Provider provider,AppSettings settings){
        for(Entry e:state.chunks){if("COMPLETED".equals(e.status))continue;BudgetPolicy.Decision budget=BudgetPolicy.beforePaidRequest(settings,state.actualCost,0,Math.max(0,e.attempts-1),true);if(!budget.allowed){state.paused=true;state.pauseReason=budget.reason;return;}
            for(int attempt=e.attempts+1;attempt<=Math.max(1,settings.maxAttempts);attempt++){e.attempts=attempt;e.status="REQUESTING";try{Reply reply=provider.request(e.index,attempt);e.status="RESPONSE_RECEIVED";state.actualCost+=Math.max(0,reply.cost);ResponseValidator.Result v=ResponseValidator.validate(reply.text,reply.text,e.source,"",reply.finish,reply.tokens,settings.maxOutputTokens,false);if(!v.accepted){e.status="RETRYABLE_ERROR";e.error=v.summary();continue;}e.candidate=reply.text;e.accepted=e.candidate;e.status="COMPLETED";e.error="";break;}catch(Exception x){e.status="RETRYABLE_ERROR";e.error=x.getMessage();}}
            if(!"COMPLETED".equals(e.status))return;
        }
    }
    public static void recoverAfterProcessDeath(State s){for(Entry e:s.chunks)if("REQUESTING".equals(e.status)){e.status="RETRYABLE_ERROR";e.error="PROCESS_DEATH";}}
    public static void acceptCandidate(Entry e,String candidate){ResponseValidator.Result v=ResponseValidator.validate(candidate,candidate,e.source,"","stop",0,0,false);if(!v.accepted)throw new IllegalArgumentException(v.summary());e.candidate=candidate;e.accepted=candidate;e.status="COMPLETED";}
    private ReliabilityScenarioRunner(){}
}

package com.ml.tblandroidtxt;
import android.app.*;
import android.content.*;
import android.os.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import com.ml.tblandroidtxt.EditorialPairModels.*;

/** Foreground owner of Editorial execution. Screens observe the existing store; never auto-restarts paid work. */
public final class EditorialPairForegroundService extends Service {
    static final String EXTRA_RUN="editorialRunId", EXTRA_FINISHED="editorialFinished";
    private static final String CHANNEL="editorial_progress";
    private static final int NOTIFICATION=422;
    private static final Set<Long> QUEUED=ConcurrentHashMap.newKeySet();
    private static final Set<Long> CANCEL=ConcurrentHashMap.newKeySet();
    private static final Map<Long,EditorialPairRunService> ACTIVE=new ConcurrentHashMap<>();
    public static boolean isActive(long id){return QUEUED.contains(id)||ACTIVE.containsKey(id);}
    static void start(Context context,long id){
        if(!QUEUED.add(id))return;
        try {context.startForegroundService(new Intent(context,EditorialPairForegroundService.class).putExtra(EXTRA_RUN,id));}
        catch(RuntimeException failure){QUEUED.remove(id);throw failure;}
    }
    static void cancelRun(long id){CANCEL.add(id);EditorialPairRunService service=ACTIVE.get(id);if(service!=null)service.cancel();}
    @Override public IBinder onBind(Intent intent){return null;}
    @Override public void onCreate(){super.onCreate();NotificationManager manager=getSystemService(NotificationManager.class);if(manager!=null)manager.createNotificationChannel(new NotificationChannel(CHANNEL,"Biên tập",NotificationManager.IMPORTANCE_LOW));}
    @Override public int onStartCommand(Intent intent,int flags,int startId){
        long id=intent==null?0:intent.getLongExtra(EXTRA_RUN,0);if(id<=0){stopSelf(startId);return START_NOT_STICKY;}
        if(ACTIVE.containsKey(id))return START_NOT_STICKY;
        QUEUED.add(id);
        startForeground(NOTIFICATION,notification("Đang chuẩn bị Biên tập",0,0));
        new Thread(()->execute(id,startId),"editorial-job-"+id).start();
        return START_NOT_STICKY;
    }
    private void execute(long id,int startId){
        PowerManager.WakeLock wake=null;
        boolean duplicate=false;
        try(SqliteEditorialPairRunStore store=new SqliteEditorialPairRunStore(this)){
            PairRun run=store.getRun(id);if(run==null)return;
            AppSettings settings=SettingsStore.load(this);EditorialPreferences choices=EditorialPreferences.frozen(run.cueFields);
            EditorialApiProvider provider=EditorialApiUiController.providerOverride!=null?EditorialApiUiController.providerOverride:new OpenRouterEditorialApiProvider(settings,choices.reasoning,true);
            EditorialPairRunService service=new EditorialPairRunService(store,provider,new EditorialApiModelPricing(settings.provider),EditorialApiUiController.REQUEST_TIMEOUT_MILLIS);
            if(ACTIVE.putIfAbsent(id,service)!=null){duplicate=true;return;}
            PowerManager power=getSystemService(PowerManager.class);if(power!=null){wake=power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"TranslateBooks:Editorial");wake.acquire(60*60*1000L);}
            signal(store.getRun(id),store.items(id),false);
            service.execute(id,new EditorialPairRunService.Listener(){
                public void onPair(PairRun current,PairItem item){if(CANCEL.contains(id))service.cancel();signal(store.getRun(id),store.items(id),false);}
                public void onFinished(PairRun current){}
            });
            signal(store.getRun(id),store.items(id),true);
        }catch(RuntimeException failure){
            android.util.Log.w("EditorialJob","Execution stopped: "+failure.getClass().getSimpleName());
            sendBroadcast(new Intent(TranslatorService.ACTION_PROGRESS).setPackage(getPackageName()).putExtra(EXTRA_RUN,id).putExtra(EXTRA_FINISHED,true));
        }finally{
            if(wake!=null&&wake.isHeld())wake.release();
            if(!duplicate){
                ACTIVE.remove(id);QUEUED.remove(id);CANCEL.remove(id);
                sendBroadcast(new Intent(TranslatorService.ACTION_PROGRESS).setPackage(getPackageName()).putExtra(EXTRA_RUN,id).putExtra(EXTRA_FINISHED,true));
                if(ACTIVE.isEmpty()&&QUEUED.isEmpty()){stopForeground(STOP_FOREGROUND_REMOVE);stopSelf(startId);}
            }
        }
    }
    private void signal(PairRun run,List<PairItem> items,boolean finished){
        if(run==null)return;NotificationManager manager=getSystemService(NotificationManager.class);
        if(manager!=null)manager.notify(NOTIFICATION,notification(EditorialJobPresentation.headline(run,items,!finished),EditorialJobPresentation.done(items),items.size()));
        sendBroadcast(new Intent(TranslatorService.ACTION_PROGRESS).setPackage(getPackageName()).putExtra(EXTRA_RUN,run.id).putExtra(EXTRA_FINISHED,finished));
    }
    private Notification notification(String text,int done,int total){
        PendingIntent open=PendingIntent.getActivity(this,NOTIFICATION,new Intent(this,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this,CHANNEL).setContentTitle("Biên tập · Translate Books").setContentText(text).setSmallIcon(android.R.drawable.stat_sys_upload).setContentIntent(open).setOnlyAlertOnce(true).setOngoing(true).setProgress(total,done,total==0).build();
    }
    @Override public void onDestroy(){for(EditorialPairRunService service:ACTIVE.values())service.cancel();super.onDestroy();}
    @Override public void onTimeout(int startId,int type){for(EditorialPairRunService service:ACTIVE.values())service.cancel();stopSelf(startId);}
}

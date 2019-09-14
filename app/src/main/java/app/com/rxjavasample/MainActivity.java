package app.com.rxjavasample;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private static String TAG = "MainActivity";

    //Disposeables helps to destro or clear the observers that no longer needed after finishing a task
    CompositeDisposable disposable = new CompositeDisposable();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Summary
        //Create an Observable
        //Apply an operator to the Observable
        //Designate what thread to do the work on and what thread to emit the results to
        //Subscribe an Observer to the Observable and view the results

        Observable<Task> taskObservable = Observable //create a new Observable object
                .fromIterable(DataSource.createTasksList()) //apply 'fromIterable' operator
                .subscribeOn(Schedulers.io()) //designate worker thread (background)
                .filter(new Predicate<Task>() { // This will do the work in the back thread and ui will not be freezed
                    @Override
                    public boolean test(Task task) throws Exception {
                        Log.d(TAG, "onNext: "+Thread.currentThread().getName());
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        return task.isComplete();
                    }
                })
                .observeOn(AndroidSchedulers.mainThread()); //designate observer thread (main thread)

        //Now that I have an object to observe, I can subscribe to it with an observer.
        taskObservable.subscribe(new Observer<Task>() {
            @Override
            public void onSubscribe(Disposable d) {
                Log.d(TAG, "onSubscribe: ");
            }

            @Override
            public void onNext(Task task) {
                Log.d(TAG, "onNext: called");
                Log.d(TAG, "onNext: "+task.getDescription());
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "onError: "+e );
            }

            @Override
            public void onComplete() {
                Log.d(TAG, "onComplete: ");
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposable.clear();
    }
}

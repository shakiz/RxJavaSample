package app.com.rxjavasample.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.os.Bundle;
import android.util.Log;
import java.util.List;
import java.util.Random;
import app.com.rxjavasample.DataSource;
import app.com.rxjavasample.PostRecyclerAdapter;
import app.com.rxjavasample.R;
import app.com.rxjavasample.models.Comment;
import app.com.rxjavasample.models.Post;
import app.com.rxjavasample.models.Task;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {

    private static String TAG = "MainActivity";
    private RecyclerView recyclerView;

    //Disposeables helps to destro or clear the observers that no longer needed after finishing a task
    private CompositeDisposable disposable = new CompositeDisposable();

    private PostRecyclerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //region init ui components
        initUI();
        initRecyclerView();
        //endregion

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
//                        try {
//                            Thread.sleep(1000);
//                        } catch (InterruptedException e) {
//                            e.printStackTrace();
//                        }
                        return task.isComplete();
                    }
                })
                .observeOn(AndroidSchedulers.mainThread()); //designate observer thread (main thread)

        //region Now that I have an object to observe, I can subscribe to it with an observer.
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
        //endregion

        //region flatMap() example
        /*
        * Suppose we have to collect data from two different sources. For example,
        * Product title and will come from an end point and price will come from another end point.
        * So we have to make teo different queries in two different source
        * And that is the case when flatMap() helps the most. Flatmap is useful when we have to query into two different source for one purpose.
        * Underneath a example about this fact,
        * 1. Hit 1 : https://jsonplaceholder.typicode.com/posts/
        * 2. Hit 2 : https://jsonplaceholder.typicode.com/posts/id/comments
        */
        //First get the posts observable
        //Then get the comments observable and update the posts with the comments
        getPostsObservable()
                .subscribeOn(Schedulers.io())
                .flatMap(new Function<Post, ObservableSource<Post>>() {
            @Override
            public ObservableSource<Post> apply(Post post) throws Exception {
                return getCommentsObservable(post);
            }
        })
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Observer<Post>() {
            @Override
            public void onSubscribe(Disposable d) {
                disposable.add(d);
            }

            @Override
            public void onNext(Post post) {
                adapter.updatePost(post);
            }

            @Override
            public void onError(Throwable e) {
                Log.e(TAG, "onError: ", e.fillInStackTrace());
            }

            @Override
            public void onComplete() {
                Log.i(TAG, "onComplete: Completed");
            }
        });
        //endregion
    }

    //region get posts observable
    private Observable<Post> getPostsObservable(){
        return ApiService.getRequestApi()
                .getPosts()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap(new Function<List<Post>, ObservableSource<Post>>() {
                    @Override
                    public ObservableSource<Post> apply(List<Post> posts) throws Exception {
                        adapter.setPosts(posts);
                        return Observable.fromIterable(posts)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread());
                    }
                });
    }
    //endregion

    //region get comments observable
    private Observable<Post> getCommentsObservable(final Post post){
        return ApiService.getRequestApi()
                .getComments(post.getId())
                .map(new Function<List<Comment>, Post>() {
                    @Override
                    public Post apply(List<Comment> comments) throws Exception {
                        int delay = (new Random()).nextInt(4)+1;
                        Thread.sleep(delay);
                        Log.d(TAG, "Thread : "+Thread.currentThread().getName()+" Sleeping by "+delay+" ms");
                        post.setComments(comments);
                        return post;
                    }
                });
    }
    //endregion

    //region init UI components
    private void initUI(){
        recyclerView = findViewById(R.id.mRecyclerView);
    }
    private void initRecyclerView(){
        adapter = new PostRecyclerAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }
    //endregion

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposable.clear();
    }
}

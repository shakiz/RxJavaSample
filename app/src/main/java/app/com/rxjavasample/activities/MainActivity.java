package app.com.rxjavasample.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.os.Bundle;
import android.util.Log;
import com.jakewharton.rxbinding3.view.RxView;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import app.com.rxjavasample.DataSource;
import app.com.rxjavasample.PostRecyclerAdapter;
import app.com.rxjavasample.R;
import app.com.rxjavasample.models.Comment;
import app.com.rxjavasample.models.Post;
import app.com.rxjavasample.models.Task;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;
import kotlin.Unit;

public class MainActivity extends AppCompatActivity {

    private static String TAG = "MainActivity";
    private static String TAG_CREATE_OPERATOR = "CREATE_OPERATOR";
    private static String TAG_JUST_OPERATOR = "JUST_OPERATOR";
    private static String TAG_FLATMAP_OPERATOR = "FLATMAP_OPERATOR";
    private static String TAG_RANGE_OPERATOR = "RANGE_OPERATOR";
    private static String TAG_BUFFER_OPERATOR = "BUFFER_OPERATOR";
    private static String TAG_DEBOUNCE_OPERATOR = "DEBOUNCE_OPERATOR";
    private RecyclerView recyclerView;
    private SearchView searchView;

    //Disposeables helps to destroy or clear the observers that no longer needed after finishing a task
    private CompositeDisposable disposable = new CompositeDisposable();

    private long timeSinceLastRequest;
    private PostRecyclerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //region init ui components
        initUI();
        bindUiWithComponents();
        initRecyclerView();
        //endregion
    }

    //region perform all UI operations
    private void bindUiWithComponents(){
        timeSinceLastRequest = System.currentTimeMillis();

        //region fromIterable
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
        //endregion

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
                        //This return will give the updated posts along with the comments
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
                        //update post
                        Log.i(TAG_FLATMAP_OPERATOR, "onNext: "+post.getId());
                        updatePost(post);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.e(TAG_FLATMAP_OPERATOR, "onError: ", e.fillInStackTrace());
                    }

                    @Override
                    public void onComplete() {
                        Log.i(TAG_FLATMAP_OPERATOR, "onComplete: Completed");
                    }
                });
        //endregion

        //region create operator example
        Observable<Task> createOperatorObservable = Observable.create(new ObservableOnSubscribe<Task>() {
            @Override
            public void subscribe(ObservableEmitter<Task> emitter) throws Exception {
                for (int start = 0; start < DataSource.createTasksList().size(); start++) {
                    if (!emitter.isDisposed()){
                        emitter.onNext(DataSource.createTasksList().get(start));
                    }
                }
                if (!emitter.isDisposed()){
                    emitter.onComplete();
                }
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread());

        createOperatorObservable.subscribe(new Observer<Task>() {
            @Override
            public void onSubscribe(Disposable d) {
                disposable.add(d);
            }

            @Override
            public void onNext(Task task) {
                Log.i(TAG_CREATE_OPERATOR, "onNext: "+task.getDescription());
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });
        //endregion

        //region just operator
        //Just operator creates just a single observable or small list of observable
        Observable
                .just(DataSource.createTasksList().get(0))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Task>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable.add(d);
                    }

                    @Override
                    public void onNext(Task task) {
                        Log.i(TAG_JUST_OPERATOR, "onNext: "+task.getDescription());
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
        //endregion

        //region range operator
        //1.It is useful when we try to do some heavy task into a loop , like looping through 1-10 range and performing some operations.
        //2.range() operator can not work alone, it work with some other helping hand like map() or takeWhile() operator
        //In this example,I am creating a work where the range should be 0-8 and the map() operator only work when task is completed.
        Observable<Task> rangerObservable = Observable
                .range(0,9)
                .subscribeOn(Schedulers.io())
                .map(new Function<Integer, Task>() {
                    @Override
                    public Task apply(Integer integer) throws Exception {
                        Log.i(TAG_RANGE_OPERATOR, "appliedThread: "+Thread.currentThread().getName());
                        return new Task("This is a demo task from range operator",true,2);
                    }
                })
                .takeWhile(new Predicate<Task>() {
                    @Override
                    public boolean test(Task task) throws Exception {
                        return task.isComplete();
                    }
                })
                .observeOn(AndroidSchedulers.mainThread());
        rangerObservable.subscribe(new Observer<Task>() {
            @Override
            public void onSubscribe(Disposable d) {
                disposable.add(d);
            }

            @Override
            public void onNext(Task task) {
                Log.i(TAG_RANGE_OPERATOR, "onNext: "+task.getDescription());
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });
        //endregion

        //region buffer operator example
        //1.It simply means buffering emitting objects
        //2.It collects a group of objects and emits them within a time intervals
        //3.Following is a example that detects button click in each 4 seconds later.Its like tracking UI interactions
        RxView.clicks(findViewById(R.id.demoButton))
                .map(new Function<Unit, Object>() {
                    @Override
                    public Object apply(Unit unit) throws Exception {
                        return 1;
                    }
                })
                .buffer(4)//Here we applied 4 seconds timing.Which will detect number of clicks in this button in 4 secods later.
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<List<Object>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        disposable.add(d);
                    }

                    @Override
                    public void onNext(List<Object> objects) {
                        Log.i(TAG_BUFFER_OPERATOR, "onNext: Total clicks on button "+objects.size()+" times in 4 seconds.");
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
        //endregion

        //region debounce operator
        //1.Debounce add a delay in request.
        //2.Like instagram search , where a delay was made in each request which ensures less server request.
        Observable<String> debounceStringObservable = Observable.create(new ObservableOnSubscribe<String>() {
            @Override
            public void subscribe(final ObservableEmitter<String> emitter) throws Exception {
                searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                    @Override
                    public boolean onQueryTextSubmit(String query) {
                        return false;
                    }

                    @Override
                    public boolean onQueryTextChange(String newText) {
                        if (!emitter.isDisposed()){
                            emitter.onNext(newText);
                        }
                        return false;
                    }
                });
            }
        })
                .debounce(500, TimeUnit.MILLISECONDS) //Apply delay of 0.5 seconds
                .subscribeOn(Schedulers.io());
        debounceStringObservable.subscribe(new Observer<String>() {
            @Override
            public void onSubscribe(Disposable d) {
                disposable.add(d);
            }

            @Override
            public void onNext(String searchText) {
                Log.i(TAG_DEBOUNCE_OPERATOR, "onNext: "+String.valueOf(System.currentTimeMillis() - timeSinceLastRequest));
                Log.i(TAG_DEBOUNCE_OPERATOR, "timeSinceLastRequest: "+timeSinceLastRequest);
                Log.i(TAG_DEBOUNCE_OPERATOR, "onNext: Query is :  "+searchText);
                search(searchText);
            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onComplete() {

            }
        });
        //endregion
    }
    //endregion

    //region executes request searching in server
    private void search(String query){

    }
    //endregion

    //region update post into adapter
    private void updatePost(Post post){
        adapter.updatePost(post);
    }
    //endregion

    //region get posts observable
    //This will execute the retrofit query and avail the posts from server
    private Observable<Post> getPostsObservable(){
        return ApiService.getRequestApi()
                .getPosts()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMap(new Function<List<Post>, ObservableSource<Post>>() {
                    @Override
                    public ObservableSource<Post> apply(List<Post> posts) throws Exception {
                        adapter.setPosts(posts);
                        //return the observable by converting the list of posts into observable by using fromIterable operator
                        return Observable.fromIterable(posts)
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread());
                    }
                });
    }
    //endregion

    //region get comments observable by using map operator
    private Observable<Post> getCommentsObservable(final Post post){
        return ApiService.getRequestApi()
                .getComments(post.getId())
                .map(new Function<List<Comment>, Post>() {
                    @Override
                    public Post apply(List<Comment> comments) throws Exception {
                        int delay = (new Random()).nextInt(5)+1;
                        Thread.sleep(delay);
                        Log.i(TAG, "Thread : "+Thread.currentThread().getName()+" Sleeping by "+delay+" ms");
                        post.setComments(comments);
                        return post;
                    }
                })
                .subscribeOn(Schedulers.io());
    }
    //endregion

    //region init UI components
    private void initUI(){
        recyclerView = findViewById(R.id.mRecyclerView);
        searchView = findViewById(R.id.searchView);
    }
    private void initRecyclerView(){
        adapter = new PostRecyclerAdapter();
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }
    //endregion

    //region activity onDestroy() method
    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposable.clear();
    }
    //endregion
}

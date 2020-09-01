package app.com.rxjavasample;

import java.util.List;
import app.com.rxjavasample.models.Comment;
import app.com.rxjavasample.models.Post;
import io.reactivex.Observable;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface APIInterface {
    @GET("posts")
    Observable<List<Post>> getPosts();

    @GET("posts/{id}/comments")
    Observable<List<Comment>> getComments(
            @Path("id") int id
    );
}

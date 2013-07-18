package com.daifan.activity.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.*;
import android.widget.*;
import android.widget.RelativeLayout.LayoutParams;

import com.daifan.R;
import com.daifan.Singleton;
import com.daifan.activity.ImagesActivity;
import com.daifan.domain.Comment;
import com.daifan.domain.Post;
import com.daifan.domain.User;
import com.daifan.service.ImageLoader;
import com.daifan.service.PostService;

import java.util.ArrayList;

/**
 * Created by ronghao on 6/23/13.
 */
public class PostAdapter extends BaseAdapter {

    private Activity activity;
    private ArrayList<Post> posts = new ArrayList<Post>();
    private static LayoutInflater inflater = null;
    private static CommentComp commentComp = null;
    public ImageLoader imageLoader;

    public PostAdapter(Activity activity, ArrayList<Post> posts) {
        this.activity = activity;
        this.posts = posts;
        inflater = (LayoutInflater) activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        imageLoader = Singleton.getInstance().getImageLoader();
    }

    @Override
    public int getCount() {
        return posts.size();
    }

    @Override
    public Object getItem(int i) {
        return posts.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View vi, ViewGroup viewGroup) {

        if (commentComp == null)
            commentComp = new CommentComp(activity);

        if (vi == null)
            vi = inflater.inflate(R.layout.list_row, null);

        TextView title = (TextView) vi.findViewById(R.id.title); // title
        TextView desc = (TextView) vi.findViewById(R.id.desc); // artist name
        ImageView thumb_image = (ImageView) vi.findViewById(R.id.thumbnail); // thumb image
        TextView createdAtTxt = (TextView) vi.findViewById(R.id.createdAt);


        final Post post = posts.get(i);

        imageLoader.DisplayImage(post.getThumbnailUrl(), thumb_image);

        title.setText(post.getUserName() );
        desc.setText(post.getName() + " " + post.getDesc());

        long time = post.getCreatedAt().getTime();
        Log.d(Singleton.DAIFAN_TAG, "created at " + post.getCreatedAt());
        createdAtTxt.setText(DateUtils.getRelativeTimeSpanString(time, System.currentTimeMillis(), 0));

        ImageView imageV = (ImageView) vi.findViewById(R.id.list_row_image);
        if (post.getImages().length == 0)
            imageV.setVisibility(View.GONE);
        else
            this.imageLoader.DisplayImage(Post.thumb(post.getImages()[0]), imageV);

        imageV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent login = new Intent(activity.getApplicationContext(), ImagesActivity.class);
                login.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                login.putExtra("images", post.getImages());
                activity.startActivity(login);
                imageLoader.preload(post.getImages());
            }
        });


        final TextView bookedUNameTxt = (TextView) vi.findViewById(R.id.booked_uname_txts);
        final TextView bookedNameLabel = (TextView) vi.findViewById(R.id.booked_uname_label);
        reLayoutBooked(post, bookedUNameTxt);

        final RelativeLayout commentContainers = (RelativeLayout) vi.findViewById(R.id.list_row_comments_container);
        commentContainers.removeViews(0, commentContainers.getChildCount());

        if (post.getComments().size() > 0) {
            View pre = bookedNameLabel;
            for (Comment cm : post.getComments()) {
                pre = appendComment(commentContainers, cm, pre);
            }
            commentContainers.setVisibility(View.VISIBLE);
        }

        final ImageButton bookBtn = (ImageButton) vi.findViewById(R.id.btnBooked);
        final User currU = Singleton.getInstance().getCurrUser();
        boolean booked = (currU == null ? false : post.booked(currU.getId()));
        if (booked) {
           // bookBtn.setImageDrawable(R.d);
        }
        if (post.outofOrder()) {
            bookBtn.setImageDrawable(activity.getResources().getDrawable(R.drawable.book_outoforder));
        }

        bookBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (post.outofOrder() && !post.booked(currU.getId())) {
                    Toast.makeText(activity, R.string.out_of_order, Toast.LENGTH_LONG).show();
                    bookBtn.setImageDrawable(activity.getResources().getDrawable(R.drawable.book_outoforder));
                    return;
                }

                if (currU == null) {
                    Toast.makeText(activity, R.string.login_required, Toast.LENGTH_LONG).show();
                    return;
                }

                if (post.booked(currU.getId()))
                    post.undoBook(currU);
                else
                    post.addBooked(currU);


                final boolean nowBooked = post.booked(currU.getId());
//                bookBtn.setHint(nowBooked ? R.string.bookBtn_cancel : R.string.bookBtn_book);
                if (post.outofOrder())
                    bookBtn.setImageDrawable(activity.getResources().getDrawable(R.drawable.book_outoforder));

                reLayoutBooked(post, bookedUNameTxt);

                new AsyncTask<Void, Void, Boolean>() {
                    @Override
                    protected Boolean doInBackground(Void... params) {
                        PostService postService = Singleton.getInstance().getPostService();
                        return nowBooked ?
                                postService.undoBook(post)
                                : postService.book(post);
                    }

                    @Override
                    protected void onPostExecute(Boolean result) {
                        Log.i(Singleton.DAIFAN_TAG, "onPostExecute of book:" + result);
                    }
                }.execute();
            }
        });

        final ImageButton commentBtn = (ImageButton) vi.findViewById(R.id.btnComment);

        commentBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                commentComp.showForPost(post, PostAdapter.this);
            }
        });

        return vi;
    }

    private void reLayoutBooked(Post post, TextView bookedUNameTxt) {

        if (bookedUNameTxt == null) {
            Log.e(Singleton.DAIFAN_TAG, "booked name text view is null");
            return;
        }

        if (post.getBookedUids().length > 0) {
            bookedUNameTxt.setText(post.getBookedUNames());
            Log.d(Singleton.DAIFAN_TAG, "refresh booked names for post " + post.getId() + ", names:" + post.getBookedUNames());
        }
    }

    private TextView appendComment(RelativeLayout commentContainers, Comment cm, View pre) {
        TextView textLabel = (TextView) new TextView(activity);
        TextView textView = new TextView(activity);

        LayoutParams p1 = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        p1.addRule(RelativeLayout.BELOW, pre != null ? pre.getId() : R.id.booked_uname_label);
        textLabel.setLayoutParams(p1);
        textLabel.setId(pre != null? pre.getId()+1 : 1);
        textLabel.setTextColor(activity.getResources().getColor(R.color.post_anota_num_color));
        textLabel.setPadding(5,2,5,2);

        LayoutParams p2 = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,  ViewGroup.LayoutParams.WRAP_CONTENT);
        p2.addRule(RelativeLayout.BELOW, pre != null ? pre.getId() : R.id.booked_uname_label);
        p2.addRule(RelativeLayout.RIGHT_OF, textLabel.getId());
        p2.addRule(RelativeLayout.ALIGN_TOP, textLabel.getId());
        textView.setLayoutParams(p2);

        textLabel.setText(Singleton.getInstance().getUNameById(String.valueOf(cm.getUid())) + ": ");
        textView.setText(cm.getComment());
        commentContainers.addView(textLabel);
        commentContainers.addView(textView);
        return textLabel;
    }
}

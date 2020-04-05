package com.culmyca.ec_2020_app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager.widget.ViewPager;

import com.culmyca.ec_2020_app.adapter.EventsAdapter2;
import com.culmyca.ec_2020_app.adapter.RoundStoryAdapter;
import com.culmyca.ec_2020_app.adapter.ViewPagerAdapter;
import com.culmyca.ec_2020_app.story.stories_main;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.snackbar.Snackbar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    Timer timer;
    Context ctx;
    int currentPosition = 0;
    ViewPager viewPager;
    LinearLayout linearLayout;
    RecyclerView stories;

    static ArrayList<ArrayList<String>> sortedAllLinks;//Get value of all links from database
    static ArrayList<String> sortedFrom;//Get value from database
    static ArrayList<ArrayList<String>> sortedTime;//Format - "dd-mm-yyyy hh:ss"//24hrs system
    static ArrayList<Boolean> sortedState;//True if there is new story from a specific club
    static int sortedPosition[];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction().replace(R.id.frame, new homepage()).commit();
        }

        SharedPreferences sharedPreferences = this.getSharedPreferences("com.example.ec_2020_app.Login", Context.MODE_PRIVATE);
        if(sharedPreferences.getString("isFirst2nd",null)!=null)
        {
            Snackbar.make(findViewById(android.R.id.content), "Welcome Back !!", Snackbar.LENGTH_SHORT).show();
        }
        sharedPreferences.edit().putString("isFirst2nd", "yo").apply();

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_nav);
        bottomNavigationView.setOnNavigationItemSelectedListener(navigationItemSelectedListener);
        //  log_out = findViewById(R.id.log_out);
        getSupportActionBar().hide();
        //   show_name = findViewById(R.id.fetch_name);
        //   show_college = findViewById(R.id.fetch_college);
        //    show_email = findViewById(R.id.fetch_email);
        //   show_mobile = findViewById(R.id.fetch_mobile);
        bottomNavigationView.setSelectedItemId(R.id.home_icon);

        galleryViewPager();

        //RecyclerView recyclerView = findViewById(R.id.recycler1);
        stories = findViewById(R.id.recycler1);
        stories.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        RecyclerView recyclerView2 = findViewById(R.id.recycler2);
        ctx = this;
        storyProcessor();
        stories.setAdapter(new RoundStoryAdapter(this, sortedFrom, sortedState));
        stories.addOnItemTouchListener(
                new RecyclerItemClickListener(ctx, stories, new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        storyOnClick(position);
                    }

                    @Override
                    public void onLongItemClick(View view, int position) {
                    }
                })
        );

        recyclerView2.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        recyclerView2.setAdapter(new EventsAdapter2(ctx));


    }

    private BottomNavigationView.OnNavigationItemSelectedListener navigationItemSelectedListener =
            new BottomNavigationView.OnNavigationItemSelectedListener() {
                @Override
                public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                    Fragment fragment = null;
                    switch (item.getItemId()) {


                        case R.id.home_icon:
                            fragment = new homepage();
                            break;

                        case R.id.developers_icon:
                            fragment = new developers();
                            // startActivity(new Intent(MainActivity.this,developers.class));
                            break;
                        case R.id.about_sec:
                            fragment = new about_sec();
                            break;


                        default:
                            break;

                    }
                    getSupportFragmentManager().beginTransaction().replace(R.id.frame, fragment).commit();
                    return true;
                }
            };

    private void galleryViewPager() {

        linearLayout = findViewById(R.id.dotsIndicator);
        viewPager = findViewById(R.id.gallery);

        viewPager.setAdapter(new ViewPagerAdapter(this));


        createSlideShow();

        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                currentPosition = position;
            }

            @Override
            public void onPageSelected(int position) {
                prepareDots(position);
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

    }


    private void createSlideShow() {
        final Handler handler = new Handler();
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {

                if (currentPosition == 8) // Prev Max = 5
                    currentPosition = 0;
                viewPager.setCurrentItem(currentPosition++, true);


            }
        };
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                handler.post(runnable);
            }
        }, 0, 1000);

    }

    private void prepareDots(int currentSlidePosition) {
        if (linearLayout.getChildCount() > 0)
            linearLayout.removeAllViews();

        ImageView dots[] = new ImageView[8];

        for (int i = 0; i < 8; i++) {
            dots[i] = new ImageView(this);
            if (i == currentSlidePosition)
                dots[i].setImageDrawable(ContextCompat.getDrawable(this, R.drawable.activedots));
            else
                dots[i].setImageDrawable(ContextCompat.getDrawable(this, R.drawable.inactivedots));

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParams.setMargins(4, 0, 4, 0);
            linearLayout.addView(dots[i], layoutParams);

        }
    }

    private void storyProcessor() {
        ArrayList<String> seenlinks = sharedlinkstolist();//Contains all seen story links, has to be stored as SharedPreferences
        StringTokenizer str = new StringTokenizer("");
        ArrayList<ArrayList<String>> alllinks = new ArrayList<>();//Get value of all links from database
        ArrayList<String> from = new ArrayList<>();//Get value from database
        ArrayList<ArrayList<String>> time = new ArrayList<>();//Format - "dd-mm-yyyy hh:ss"//24hrs system
        ArrayList<Boolean> state = new ArrayList<>();//True if there is new story from a specific club
        ArrayList<Integer> position = new ArrayList<>();//Stores position from where stories have to be started

        //SampleData
        from.add("Manan");
        from.add("Ananya");
        from.add("IEEE");
        from.add("Jhalak");
        from.add("Microbird");
        from.add("Niramayam");
        from.add("Natraja");
        from.add("Team Mechnext Racing");
        from.add("Vividha");


        ArrayList<String> pp = new ArrayList<>();
//        pp.add("https://cdn.pixabay.com/photo/2020/03/07/05/18/coffee-4908764_960_720.jpg");
//        pp.add("https://cdn.pixabay.com/photo/2020/03/04/05/57/key-4900643_960_720.jpg");
        pp.add("file:///android_asset/club_images/man1-min.jpg");
        pp.add("file:///android_asset/club_images/man2-min.jpg");
        pp.add("file:///android_asset/club_images/man3-min.jpg");
        alllinks.add(pp);
        ArrayList<String> bb = new ArrayList<>();
        bb.add("file:///android_asset/club_images/an1-min.png");
        bb.add("file:///android_asset/club_images/an2-min.png");
        bb.add("file:///android_asset/club_images/an3-min.jpg");
        bb.add("file:///android_asset/club_images/an4-min.jpg");
        bb.add("file:///android_asset/club_images/an5-min.jpg");
        alllinks.add(bb);
        ArrayList<String> cc = new ArrayList<>();
        cc.add("file:///android_asset/club_images/ieee1-min.jpeg");
        cc.add("file:///android_asset/club_images/ieee2-min.jpeg");
        cc.add("file:///android_asset/club_images/ieee3-min.jpg");
        cc.add("file:///android_asset/club_images/ieee4-min.jpg");
        cc.add("file:///android_asset/club_images/ieee5-min.jpg");
        cc.add("file:///android_asset/club_images/ieee6-min.jpg");
        alllinks.add(cc);
        cc = new ArrayList<>();
        cc.add("file:///android_asset/club_images/jh1-min.JPG");
        cc.add("file:///android_asset/club_images/jh2-min.jpg");
        cc.add("file:///android_asset/club_images/jh3-min.jpeg");
        alllinks.add(cc);
        cc = new ArrayList<>();
        cc.add("file:///android_asset/club_images/mb1-min.jpg");
        cc.add("file:///android_asset/club_images/mb2-min.jpg");
        cc.add("file:///android_asset/club_images/mb3-min.jpg");
        cc.add("file:///android_asset/club_images/mb4-min.jpg");
        cc.add("file:///android_asset/club_images/mb5-min.jpg");
        cc.add("file:///android_asset/club_images/mb6-min.jpg");
        alllinks.add(cc);
        cc = new ArrayList<>();
        cc.add("file:///android_asset/club_images/nir1-min.jpg");
        cc.add("file:///android_asset/club_images/nir2-min.jpg");
        cc.add("file:///android_asset/club_images/nir3-min.JPG");
        cc.add("file:///android_asset/club_images/nir4-min.JPG");
        cc.add("file:///android_asset/club_images/nir5-min.JPG");
        cc.add("file:///android_asset/club_images/nir6-min.jpg");
        cc.add("file:///android_asset/club_images/nir7-min.jpg");
        alllinks.add(cc);
        cc = new ArrayList<>();
        cc.add("file:///android_asset/club_images/nt1-min.jpg");
        cc.add("file:///android_asset/club_images/nt2-min.jpg");
        cc.add("file:///android_asset/club_images/nt3-min.jpg");
        cc.add("file:///android_asset/club_images/nt4-min.jpg");
        cc.add("file:///android_asset/club_images/nt5-min.jpeg");
        cc.add("file:///android_asset/club_images/nt6-min.jpeg");
        cc.add("file:///android_asset/club_images/nt7-min.jpeg");
        cc.add("file:///android_asset/club_images/nt8-min.jpeg");
        alllinks.add(cc);
        cc = new ArrayList<>();
        cc.add("file:///android_asset/club_images/tmr1-min.jpg");
        cc.add("file:///android_asset/club_images/tmr2-min.jpg");
        cc.add("file:///android_asset/club_images/tmr3-min.jpg");
        cc.add("file:///android_asset/club_images/tmr4-min.jpg");
        cc.add("file:///android_asset/club_images/tmr5-min.jpg");
        alllinks.add(cc);
        cc = new ArrayList<>();
        cc.add("file:///android_asset/club_images/viv1-min.jpeg");
        cc.add("file:///android_asset/club_images/viv2-min.jpeg");
        cc.add("file:///android_asset/club_images/viv3-min.jpeg");
        cc.add("file:///android_asset/club_images/viv4-min.jpeg");
        cc.add("file:///android_asset/club_images/viv5-min.jpeg");
        alllinks.add(cc);

        String timearr[] = new String[8];
        String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        for (int i = 0; i < 8; i++) {
            int hr = Integer.parseInt(currentTime.substring(0, 2));
            if ((hr - 18 + (2 * i)) < 0)
                if (((hr - 18 + (2 * i)) + 24) < 12)
                    timearr[i] = "Yesterday, " + ((hr - 18 + (2 * i)) + 24) + ":00 a.m.";
                else
                    timearr[i] = "Yesterday, " + ((hr - 18 + (2 * i)) + 24 - 12) + ":00 p.m.";
            else if (((hr - 18 + (2 * i))) < 12)
                timearr[i] = "Today, " + ((hr - 18 + (2 * i))) + ":00 a.m.";
            else
                timearr[i] = "Today, " + ((hr - 18 + (2 * i)) - 12) + ":00 p.m.";
        }


        pp = new ArrayList<>();
        for (int i = 0; i < 3; i++)
            pp.add(timearr[i]);
        time.add(pp);
        for (int i = 3; i < 5; i++)
            pp.add(timearr[i]);
        time.add(pp);
        pp.add(timearr[5]);
        time.add(pp);
        cc = new ArrayList<>();
        for (int i = 0; i < 3; i++)
            cc.add(timearr[i]);
        time.add(cc);
        time.add(pp);
        pp.add(timearr[6]);
        time.add(pp);
        pp.add(timearr[7]);
        time.add(pp);
        pp = new ArrayList<>();
        for (int i = 0; i < 5; i++)
            pp.add(timearr[i]);
        time.add(pp);
        time.add(pp);

        //Sample Data Ends


        for (int i = 0; i < alllinks.size(); i++) {
            boolean _new = false;
            for (int j = 0; j < alllinks.get(i).size(); j++) {
                String link = alllinks.get(i).get(j);
                boolean check = !(seenlinks.contains(link));
                if (!_new && check) position.add(j);
                if (check) _new = true;
            }
            if (!_new) position.add(0);
            state.add(_new);
        }

        sortedAllLinks = new ArrayList<>();//Get value of all links from database
        sortedFrom = new ArrayList<>();//Get value from database
        sortedTime = new ArrayList<>();//Format - "dd-mm-yyyy hh:ss"//24hrs system
        sortedState = new ArrayList<>();//True if there is new story from a specific club
        sortedPosition = new int[alllinks.size()];//Stores position from where stories have to be started
        int count = 0;

        for (int i = 0; i < alllinks.size(); i++) {
            if (state.get(i)) {
                sortedAllLinks.add(alllinks.get(i));
                sortedFrom.add(from.get(i));
                sortedTime.add(time.get(i));
                sortedState.add(state.get(i));
                sortedPosition[count] = position.get(i);
                count++;
            }
        }
        for (int i = 0; i < alllinks.size(); i++) {
            if (!state.get(i)) {
                sortedAllLinks.add(alllinks.get(i));
                sortedFrom.add(from.get(i));
                sortedTime.add(time.get(i));
                sortedState.add(state.get(i));
                sortedPosition[count] = position.get(i);
                count++;
            }
        }
    }

    private void storyOnClick(int position) {
        Intent it = new Intent(ctx, stories_main.class);
        it.putExtra("url", sortedAllLinks);
        it.putExtra("allIndex", sortedPosition);
        it.putExtra("index", position);
        it.putExtra("from", sortedFrom);
        it.putExtra("time", sortedTime);
        startActivityForResult(it, 0);
        overridePendingTransition(R.anim.zoom_enter, 0);
    }

    private ArrayList<String> sharedlinkstolist() {
        ArrayList<String> visitedlinks = new ArrayList<>();
        String newsaved = "";
        StringTokenizer str = new StringTokenizer(SaveSharedPreference.getVisitedlinks(ctx).trim());
        for (int i = 0; str.hasMoreTokens(); i++) {
            String a = str.nextToken();
            if (!visitedlinks.contains(a)) {
                visitedlinks.add(a);
                newsaved = newsaved + a + " ";
            }
        }
        SaveSharedPreference.setVisitedlinks(ctx, newsaved);
        return visitedlinks;
    }

    @Override
    protected void onResume() {
        super.onResume();
        storyProcessor();
        stories.setAdapter(new RoundStoryAdapter(this, sortedFrom, sortedState));
        stories.addOnItemTouchListener(
                new RecyclerItemClickListener(ctx, stories, new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        storyOnClick(position);
                    }

                    @Override
                    public void onLongItemClick(View view, int position) {
                    }
                })
        );
    }
}

package com.johnny.draglayoutexpand;

import android.app.ListActivity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.johnny.draglayoutexpand.R;
import com.johnny.draglayoutexpand.adapter.SwipeListAdapter;
import com.johnny.draglayoutexpand.utils.Utils;
import com.johnny.draglayoutexpand.view.DragLayout;
import com.johnny.draglayoutexpand.view.DragRelativeLayout;
import com.nineoldandroids.view.ViewHelper;

public class MainActivity extends ListActivity implements View.OnClickListener {
    private DragLayout mDragLayout;
    private ImageView mHeader;
    private SwipeListAdapter adapter;
    private ListView mLeftList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.johnny.draglayoutexpand.R.layout.activity_main);
        initLeftContent();
        initMainContent();
    }

    private void initMainContent() {
        mDragLayout = (DragLayout) findViewById(R.id.dsl);
        mDragLayout.setDragListener(mDragListener);
        DragRelativeLayout mMainView = (DragRelativeLayout) findViewById(R.id.rl_main);
        mMainView.setDragLayout(mDragLayout);
        mHeader = (ImageView) findViewById(R.id.iv_head);
        mHeader.setOnClickListener(this);
        mBtRight = findViewById(R.id.iv_head_right);
        mBtRight.setOnClickListener(this);
        mHeader.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                mDragLayout.switchScaleEnable();
                return true;
            }
        });
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, Cheeses.sCheeseStrings);
        setListAdapter(arrayAdapter);
        adapter = new SwipeListAdapter(MainActivity.this);
        setListAdapter(adapter);
        mDragLayout.setAdapterInterface(adapter);
        getListView().setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if(scrollState == OnScrollListener.SCROLL_STATE_TOUCH_SCROLL){
                    adapter.closeAllLayout();
                }
            }
            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }
        });

    }

    public void initLeftContent() {
        mLeftList = (ListView) findViewById(R.id.lv_left);
        mLeftList.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, Cheeses.sCheeseStrings){
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView mText = (TextView) view.findViewById(android.R.id.text1);
                mText.setTextColor(Color.WHITE);
                return view;
            }
        });
    }

    private DragLayout.OnDragListener mDragListener = new DragLayout.OnDragListener() {
        @Override
        public void onOpen() {
        }
        @Override
        public void onClose() {
            shakeHeader();
            mBtRight.setSelected(false);
        }
        @Override
        public void onDrag(final float percent) {
            /*主界面左上角头像渐渐消失*/
            ViewHelper.setAlpha(mHeader, 1 - percent);
        }
        @Override
        public void onStartOpen(DragLayout.Direction direction) {
            Utils.showToast(getApplicationContext(), "onStartOpen: " + direction.toString());
        }
    };

    private View mBtRight;

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_head:
                mDragLayout.open(true);
                break;
            case R.id.iv_head_right:
                mDragLayout.open(true, DragLayout.Direction.Right);
                mBtRight.setSelected(true);
                break;
            default:
                break;
        }
    }

    private void shakeHeader() {
        mHeader.startAnimation(AnimationUtils.loadAnimation(this, R.anim.shake));
    }

    @Override
    public void onBackPressed() {
        if (mDragLayout.getStatus() != DragLayout.Status.Close) {
            mDragLayout.close();
            return;
        }
        super.onBackPressed();
    }
}
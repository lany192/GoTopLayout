package com.lany.layout;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * 置顶封装
 */
public class GoTopLayout extends FrameLayout {
    public static final String TAG = "GoTopLayout";
    private View mTargetView;
    private ImageView mGotoTopBtn;
    private MyScrollListener myScrollListener;
    private int arrowIcon;
    private int gotoTopVisibleItem = 5;

    public GoTopLayout(Context context) {
        super(context);
        init(context, null, 0);
    }

    public GoTopLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public GoTopLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs, defStyleAttr);
    }

    private void init(Context context, AttributeSet attrs, int defStyleAttr) {
        if (isInEditMode()) return;
        TypedArray attr = context.obtainStyledAttributes(attrs, R.styleable.GoTopLayout, defStyleAttr, 0);
        arrowIcon = attr.getResourceId(R.styleable.GoTopLayout_goto_top_icon, R.drawable.go2top);
        gotoTopVisibleItem = attr.getInt(R.styleable.GoTopLayout_goto_top_visible_item, 5);
        attr.recycle();
        myScrollListener = new MyScrollListener();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        attachAbsListView();
        attachScrollBackView();
    }

    private void attachAbsListView() {
        if (getChildCount() > 0) {
            mTargetView = findTargetScrollView(this);
        }
    }

    private View findTargetScrollView(View view) {
        if (view != null) {
            if (view instanceof AbsListView || view instanceof RecyclerView) return view;
            if (view instanceof ViewGroup) {
                View target = null;
                ViewGroup viewGroup = (ViewGroup) view;
                for (int i = 0; i < viewGroup.getChildCount(); i++) {
                    target = findTargetScrollView(viewGroup.getChildAt(i));
                }
                return target;
            }
        }
        return null;
    }

    private void attachScrollBackView() {
        mGotoTopBtn = new ImageView(getContext());
        mGotoTopBtn.setImageResource(arrowIcon);

        mGotoTopBtn.setVisibility(INVISIBLE);
        LayoutParams params = new LayoutParams(dp2px(42), dp2px(42));
        params.gravity = Gravity.BOTTOM | Gravity.END;
        params.bottomMargin = dp2px(10);
        params.rightMargin = dp2px(10);
        mGotoTopBtn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (mTargetView == null) {
                    return;
                }
                if (mTargetView instanceof AbsListView) {
                    final AbsListView listView = (AbsListView) mTargetView;
                    listView.smoothScrollToPositionFromTop(0, 0);
                    listView.setSelection(0);
                } else if (mTargetView instanceof RecyclerView) {
                    ((RecyclerView) mTargetView).scrollToPosition(0);
                }
            }
        });
        addView(mGotoTopBtn, params);
        bindScrollBack();
    }

    /**
     * 如果已经使用<code>AbsListView.setOnScrollListener()</code>设置过监听，一定要在其后面调用；
     * 如果没有使用<code>AbsListView.setOnScrollListener()</code>，就在onCreate()或者onActivityCreated()中调用即可;
     */
    public void bindScrollBack() {
        if (mTargetView != null && mGotoTopBtn != null) {
            if (mTargetView instanceof AbsListView) {
                hookScrollListenerForAbsListView();
            } else if (mTargetView instanceof RecyclerView) {
                addScrollListenerForRecyclerView();
            }
        }
    }

    private void hookScrollListenerForAbsListView() {
        try {
            //通过反射获取mOnScrollListener对象
            Field scrollListenerField = AbsListView.class.getDeclaredField("mOnScrollListener");
            scrollListenerField.setAccessible(true);
            Object object = scrollListenerField.get(mTargetView);
            //需要被代理的目前对象
            AbsListView.OnScrollListener target;
            if (object == null) {
                //如果mOnScrollListener没有设置过，就设置一个空的用来hook
                target = new FakeScrollListener();
            } else {
                target = (AbsListView.OnScrollListener) object;
            }
            //InvocationHandler对象，用于添加额外的控制处理
            ScrollListenerInvocationHandler listenerInvocationHandler = new ScrollListenerInvocationHandler(target);
            //Proxy.newProxyInstance生成动态代理对象
            AbsListView.OnScrollListener proxy = listenerInvocationHandler.getProxy();
            //将代理对象proxy设置到被反射的mOnScrollListener的字段中
            scrollListenerField.set(mTargetView, proxy);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }

    private void addScrollListenerForRecyclerView() {
        ((RecyclerView) mTargetView).addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (myScrollListener != null) {
                    myScrollListener.onScrollStateChanged(recyclerView, newState);
                }
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager manager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (manager != null) {
                    if (myScrollListener != null) {
                        myScrollListener.onScroll(recyclerView, manager.findFirstVisibleItemPosition(), manager.getChildCount(), manager.getItemCount());
                    }
                } else {
                    if (myScrollListener != null) {
                        myScrollListener.onScroll(recyclerView, dy, 0, 0);
                    }
                }
            }
        });
    }

    public boolean isHidden() {
        return mGotoTopBtn != null && mGotoTopBtn.getVisibility() == INVISIBLE;
    }

    private class MyScrollListener {
        private int scrollState;

        void onScrollStateChanged(View scrollview, int scrollState) {
            this.scrollState = scrollState;
        }

        void onScroll(View scrollview, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            if (scrollState == 0)
                return;
            if (firstVisibleItem >= gotoTopVisibleItem) {
                if (isHidden()) {
                    if (mGotoTopBtn != null) {
                        mGotoTopBtn.setVisibility(VISIBLE);
                    }
                }
            } else {
                if (!isHidden()) {
                    if (mGotoTopBtn != null) {
                        mGotoTopBtn.setVisibility(INVISIBLE);
                    }
                }
            }
        }
    }

    private class ScrollListenerInvocationHandler implements InvocationHandler {
        private AbsListView.OnScrollListener target;

        ScrollListenerInvocationHandler(AbsListView.OnScrollListener target) {
            this.target = target;
        }

        public AbsListView.OnScrollListener getProxy() {
            // Proxy.newProxyInstance() 第二个参数一定不能使用 target.getClass().getInterfaces()获得被代理的接口
            // 因为上面获得的是当前实现类本身实现的接口，不包含父类实现的接口；
            // 这里采取固定AbsListView.OnScrollListener接口的方式即可;
            return (AbsListView.OnScrollListener) Proxy.newProxyInstance(target.getClass().getClassLoader(), new Class[]{AbsListView.OnScrollListener.class}, this);
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Log.i(TAG, "动态代理拦截  method=" + method.getName());
            if (myScrollListener != null) {
                if (method.getName().equals("onScroll")) {
                    myScrollListener.onScroll((View) args[0], (int) args[1], (int) args[2], (int) args[3]);
                } else if (method.getName().equals("onScrollStateChanged")) {
                    myScrollListener.onScrollStateChanged((View) args[0], (int) args[1]);
                }
            }
            return method.invoke(target, args);
        }
    }

    private class FakeScrollListener implements AbsListView.OnScrollListener {
        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {

        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

        }
    }

    private int dp2px(float value) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value, getResources().getDisplayMetrics());
    }
}

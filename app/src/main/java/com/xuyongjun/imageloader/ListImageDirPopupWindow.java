package com.xuyongjun.imageloader;

import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.xuyongjun.imageloader.bean.FolderBean;
import com.xuyongjun.imageloader.utils.ImageLoader;

import java.util.List;

/**
 * ============================================================
 * 作 者 : XYJ
 * 版 本 ： 1.0
 * 创建日期 ： 2016/8/9 18:43
 * 描 述 ：
 * 修订历史 ：
 * ============================================================
 **/
public class ListImageDirPopupWindow extends PopupWindow
{
    private int mWidth;
    private int mHeight;
    private View mConvertView;
    private ListView mListView;
    private List<FolderBean> mDatas;


    public interface OnDirSelectedListener {
        void onSelected(FolderBean bean);
    }
    private OnDirSelectedListener mListener;

    /**
     * 设置回调监听
     * @param mListener
     */
    public void setOnSelectedListener(OnDirSelectedListener mListener)
    {
        this.mListener = mListener;
    }

    public ListImageDirPopupWindow(Context context, List<FolderBean> mDatas)
    {
        this.mDatas = mDatas;
        calWidthAndHeight(context);
        mConvertView = LayoutInflater.from(context).inflate(R.layout.popup_main, null);

        setContentView(mConvertView);
        setWidth(mWidth);
        setHeight(mHeight);

        //让其内的View能获取到事件
        setFocusable(true);//可触摸
        setTouchable(true);
        setOutsideTouchable(true);//外部可点击
        setBackgroundDrawable(new BitmapDrawable());//点击外部可消失

        setTouchInterceptor(new View.OnTouchListener()
        {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                //当移动到了外面直接dismiss PopWindow
                if (event.getAction() == MotionEvent.ACTION_OUTSIDE)
                {
                    dismiss();
                    return true;
                }
                return false;
            }
        });

        initViews(context);
        initEvent();
    }

    private void initViews(Context context)
    {
        mListView = (ListView) mConvertView.findViewById(R.id.id_list_dir);
        ListDirAdapter adapter = new ListDirAdapter(context,mDatas);
        mListView.setAdapter(adapter);
    }

    private void initEvent()
    {
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id)
            {
                //设置回调
                if (mListener != null) {
                    mListener.onSelected(mDatas.get(position));
                }
            }
        });
    }

    /**
     * 计算popupWindow的宽度和高度
     *
     * @param context
     */
    private void calWidthAndHeight(Context context)
    {
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(dm);
        mWidth = dm.widthPixels;
        mHeight = (int) (dm.heightPixels * 0.8);
    }

    private class ListDirAdapter extends ArrayAdapter<FolderBean>
    {

        private LayoutInflater mInflater;
        private List<FolderBean> mDatas;

        public ListDirAdapter(Context context, List<FolderBean> objects)
        {
            super(context, 0, objects);
            this.mDatas = objects;
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            ViewHolder holder = null;
            if (convertView == null)
            {
                holder = new ViewHolder();
                convertView = mInflater.inflate(R.layout.item_popup_main, parent, false);
                holder.mImg = (ImageView) convertView.findViewById(R.id.id_dir_item_image);
                holder.mDirCount = (TextView) convertView.findViewById(R.id.id_dir_item_count);
                holder.mDirName = (TextView) convertView.findViewById(R.id.id_dir_item_name);
                convertView.setTag(holder);
            } else
            {
                holder = (ViewHolder) convertView.getTag();
            }

            FolderBean bean = getItem(position);
            //重置,因为控件是复用的
            holder.mImg.setImageResource(R.mipmap.pictures_no);

            ImageLoader.getInstance().loadImage(bean.getFirstImgPath(), holder.mImg);
            holder.mDirName.setText(bean.getName());
            holder.mDirCount.setText(bean.getCount() + "张");
            return convertView;
        }

        private class ViewHolder
        {
            ImageView mImg;
            TextView mDirName;
            TextView mDirCount;
        }
    }
}

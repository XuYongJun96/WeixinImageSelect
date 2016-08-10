package com.xuyongjun.imageloader.adapter;

import android.content.Context;
import android.graphics.Color;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.xuyongjun.imageloader.R;
import com.xuyongjun.imageloader.utils.ImageLoader;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ============================================================
 * 作 者 : XYJ
 * 版 本 ： 1.0
 * 创建日期 ： 2016/8/9 9:09
 * 描 述 ：
 * 修订历史 ：
 * ============================================================
 **/
public class ImageAdapter extends BaseAdapter {

    private List<String> mImgPaths;
    private String mDirPath;
    private LayoutInflater mInflater;

    private int mScreenWidth;

    private static Set<String> mSeletedImgs = new HashSet<String>();

    //这里为什么要传图片的名字和图片的路径，而不用直接封装在集合中
    //因为如果图片数量比较多的话，集合会多存储图片的路径，这也是一笔不小的支出，所以我们选择拼接的方式
    public ImageAdapter(Context context, List<String> mDatas, String dirPath)
    {
        this.mImgPaths = mDatas;
        this.mDirPath = dirPath;
        mInflater = LayoutInflater.from(context);

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(dm);
        mScreenWidth = dm.widthPixels;
    }

    @Override
    public int getCount()
    {
        return mImgPaths.size();
    }

    @Override
    public Object getItem(int position)
    {
        return mImgPaths.get(position);
    }

    @Override
    public long getItemId(int position)
    {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.item_gridview, null);
        }

        final ViewHolder holder = ViewHolder.getViewHolder(convertView);

        //这里设置ImageView宽的最大值为屏幕的1/3，ImageView默认是match_parent的，
        // 而GridView就是3列的，这样对ImageLoader的压缩有显著明显
        holder.mImg.setMaxWidth(mScreenWidth/3);

        //重置状态,控件式复用的
        holder.mImg.setImageResource(R.mipmap.pictures_no);
        holder.mSelect.setImageResource(R.mipmap.picture_unselected);
        holder.mImg.setColorFilter(null);
        ImageLoader.getInstance(3, ImageLoader.Type.LIFO).loadImage(mDirPath + "/" + mImgPaths.get(position)
                , holder.mImg);

        final String filePath = mDirPath+"/"+mImgPaths.get(position);
        //选中图片
        holder.mImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                if (mSeletedImgs.contains(filePath)) {//已选中
                    mSeletedImgs.remove(filePath);
                    holder.mImg.setColorFilter(null);
                    holder.mSelect.setImageResource(R.mipmap.picture_unselected);
                } else {//未选中
                    mSeletedImgs.add(filePath);
                    holder.mImg.setColorFilter(Color.parseColor("#77000000"));
                    holder.mSelect.setImageResource(R.mipmap.pictures_selected);
                }
            }
        });
        if (mSeletedImgs.contains(filePath)) {
            holder.mImg.setColorFilter(Color.parseColor("#77000000"));
            holder.mSelect.setImageResource(R.mipmap.pictures_selected);
        }
        return convertView;
    }

    private static class ViewHolder {
        ImageView mImg;
        ImageButton mSelect;

        public static ViewHolder getViewHolder(View v)
        {
            Object tag = v.getTag();
            if (tag == null) {
                ViewHolder holder = new ViewHolder();
                holder.mImg = (ImageView) v.findViewById(R.id.id_item_image);
                holder.mSelect = (ImageButton) v.findViewById(R.id.id_item_select);
                v.setTag(tag);
                return holder;
            }
            return (ViewHolder) tag;
        }
    }
}

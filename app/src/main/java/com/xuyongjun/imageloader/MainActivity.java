package com.xuyongjun.imageloader;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.GridView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.xuyongjun.imageloader.adapter.ImageAdapter;
import com.xuyongjun.imageloader.bean.FolderBean;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private GridView mGridView;
    private ImageAdapter mImgAdapter;
    /**
     * Adapter数据
     */
    private List<String> mImgs;
    private RelativeLayout mBottomLayout;
    /**
     * 显示文件名
     */
    private TextView mDirName;
    /**
     * 显示图片数量
     */
    private TextView mDirCount;

    private File mCurrentDir;
    private int mMaxCount;

    private List<FolderBean> mFolderBeans = new ArrayList<FolderBean>();

    private ProgressDialog mProgressDialog;

    private ListImageDirPopupWindow mDirPopupWindow;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg)
        {
            mProgressDialog.dismiss();

            data2View();

            initDirPopupWindow();
        }
    };

    /**
     * 初始化PopupWindow
     */
    private void initDirPopupWindow()
    {
        mDirPopupWindow = new ListImageDirPopupWindow(this,mFolderBeans);

        mDirPopupWindow.setOnDismissListener(new PopupWindow.OnDismissListener()
        {
            @Override
            public void onDismiss()
            {
                lightOn();
            }
        });

        //设置PopupWindow选中监听事件
        mDirPopupWindow.setOnSelectedListener(new ListImageDirPopupWindow.OnDirSelectedListener()
        {
            @Override
            public void onSelected(FolderBean bean)
            {
                //关闭PopupWindow
                mDirPopupWindow.dismiss();

                mCurrentDir = new File(bean.getDir());
                mImgs = Arrays.asList(mCurrentDir.list(new FilenameFilter()
                {
                    @Override
                    public boolean accept(File dir, String filename)
                    {
                        return filename.endsWith("jpg") || filename.endsWith("jpeg") || filename.endsWith("png");
                    }
                }));

                mImgAdapter = new ImageAdapter(MainActivity.this,mImgs,bean.getDir());
                mGridView.setAdapter(mImgAdapter);

                mDirCount.setText(mImgs.size()+"张");
                mDirName.setText(bean.getName());
            }
        });

    }

    /**
     * 打开PopupWindow内容区域变暗
     * 这里是内容区域变亮
     */
    private void lightOn()
    {
        //获取Window的属性
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.alpha = 1.0f;//默认就是1.0
        getWindow().setAttributes(params);
    }

    /**
     * 内容区域变暗
     */
    private void lightOff()
    {
        //获取Window的属性
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.alpha = 0.3f;//默认就是1.0
        getWindow().setAttributes(params);
    }

    /**
     * 扫描完成后绑定数据
     */
    private void data2View()
    {
        if (mCurrentDir == null) {
            Toast.makeText(this, "未扫描到任何图片", Toast.LENGTH_SHORT).show();
            return;
        }
        mImgs = Arrays.asList(mCurrentDir.list());

        //设置适配器
        mImgAdapter = new ImageAdapter(this, mImgs, mCurrentDir.getAbsolutePath());
        mGridView.setAdapter(mImgAdapter);

        mDirCount.setText(mMaxCount + "张");
        mDirName.setText(mCurrentDir.getName());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();
        initDatas();
        initEvent();
    }

    private void initView()
    {
        mGridView = (GridView) findViewById(R.id.id_gridView);
        mBottomLayout = (RelativeLayout) findViewById(R.id.id_botton_rl);
        mDirCount = (TextView) findViewById(R.id.id_dir_count);
        mDirName = (TextView) findViewById(R.id.id_dir_name);
    }

    /**
     * 利用ContentProvider扫描手机中的所有图片
     */
    private void initDatas()
    {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Toast.makeText(this, "当前存储卡不可用", Toast.LENGTH_SHORT).show();
            return;
        }

        mProgressDialog = ProgressDialog.show(this, null, "正在加载...");

        new Thread() {
            @Override
            public void run()
            {
                Uri mImgUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                ContentResolver cr = MainActivity.this.getContentResolver();
                Cursor cursor = cr.query(mImgUri, null, MediaStore.Images.Media.MIME_TYPE + " = ? or "
                                + MediaStore.Images.Media.MIME_TYPE + " = ? ", new String[]{"image/jpeg", "image/png"},
                        MediaStore.Images.Media.DATE_MODIFIED);

                //如果一个文件夹下存在很多图片，每张图片都会获取它的父路径进行遍历，那么这样就遍历了很多次
                //我们定义Set记录遍历过了的文件夹路径，避免了重复的遍历操作
                Set<String> mDirPaths = new HashSet<String>();

                while (cursor.moveToNext()) {
                    String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                    File parentFile = new File(path).getParentFile();
                    //如果图片在根目录下的话，获取它的父File是有问题的，下面对其进行判断
                    if (parentFile == null)
                        continue;
                    String dirPath = parentFile.getAbsolutePath();

                    FolderBean folderBean = null;

                    if (mDirPaths.contains(dirPath)) {
                        continue;
                    } else {
                        mDirPaths.add(dirPath);
                        folderBean = new FolderBean();
                        folderBean.setDir(dirPath);
                        folderBean.setFirstImgPath(path);
                    }
                    //可能有些图片存在于ContentProvider中，但是文件中看不到，防止程序崩了
                    if (parentFile.list() == null)
                        continue;

                    int picSize = parentFile.list(new FilenameFilter() {

                        @Override
                        public boolean accept(File dir, String filename)
                        {
                            //过滤文件名称
                            return filename.endsWith("jpg") || filename.endsWith("jpeg") || filename.endsWith("png");
                        }
                    }).length;

                    folderBean.setCount(picSize);

                    mFolderBeans.add(folderBean);

                    //获取图片最后的文件夹及数量
                    if (picSize > mMaxCount) {
                        mMaxCount = picSize;
                        mCurrentDir = parentFile;
                    }
                }
                cursor.close();
                //扫描完成，释放临时变量的缓存
                mDirPaths = null;

                //通知Handler图片扫描完成
                handler.sendEmptyMessage(0x0001);
            }
        }.start();
    }

    private void initEvent()
    {
        mBottomLayout.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                mDirPopupWindow.setAnimationStyle(R.style.dir_popupwindow_anim);
                mDirPopupWindow.showAsDropDown(mBottomLayout,0,0);
                lightOff();
            }
        });
    }

}

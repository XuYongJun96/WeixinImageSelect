package com.xuyongjun.imageloader.bean;

/**
 * ============================================================
 * 作 者 : XYJ
 * 版 本 ： 1.0
 * 创建日期 ： 2016/8/8 23:04
 * 描 述 ：PopupWindow中列表的moudle
 * 修订历史 ：
 * ============================================================
 **/
public class FolderBean {
    /**
     * 当前文件夹的路径
     */
    private String dir;
    /**
     * 第一张图片的路径
     */
    private String firstImgPath;
    /**
     * 文件夹的名称
     */
    private String name;
    /**
     * 图片的数量
     */
    private int count;

    public String getDir()
    {
        return dir;
    }

    public void setDir(String dir)
    {
        this.dir = dir;
        int lastIndexOf = this.dir.lastIndexOf("/");
        this.name = this.dir.substring(lastIndexOf);
    }

    public String getFirstImgPath()
    {
        return firstImgPath;
    }

    public void setFirstImgPath(String firstImgPath)
    {
        this.firstImgPath = firstImgPath;
    }

    public String getName()
    {
        return name;
    }

    public int getCount()
    {
        return count;
    }

    public void setCount(int count)
    {
        this.count = count;
    }
}

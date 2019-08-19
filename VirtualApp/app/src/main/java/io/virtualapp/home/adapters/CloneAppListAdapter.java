package io.virtualapp.home.adapters;

import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import io.island.R;
import io.virtualapp.abs.ui.VUiKit;
import io.virtualapp.glide.GlideUtils;
import io.virtualapp.home.models.AppInfo;
import io.virtualapp.widgets.DragSelectRecyclerViewAdapter;
import io.virtualapp.widgets.LabelView;

/**
 * @author Lody
 */
public class CloneAppListAdapter extends DragSelectRecyclerViewAdapter<CloneAppListAdapter.ViewHolder> {

    private static final int TYPE_FOOTER = -2;
    private final View mFooterView;
    private LayoutInflater mInflater;
    private List<AppInfo> mAppList;
    private ItemEventListener mItemEventListener;

    private List<String> blacklist = new ArrayList<>();

    private Context mContext;
    private File mFrom;


    public CloneAppListAdapter(Context context, @Nullable File from) {
        //引用黑名单，仅仅只是克隆的时候不显示；安装成功后，还是会显示出来的。
        initblacklist();

        mContext = context;
        mFrom = from;
        this.mInflater = LayoutInflater.from(context);
        mFooterView = new View(context);
        StaggeredGridLayoutManager.LayoutParams params = new StaggeredGridLayoutManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, VUiKit.dpToPx(context, 60)
        );
        params.setFullSpan(true);
        mFooterView.setLayoutParams(params);
    }

    private void initblacklist(){
        //宁波银行【有xposed检测加自我校验】
        blacklist.add("com.nbbank");

        //浦发银行【有xposed检测加自我校验】反编译后，啥都没有。。
        blacklist.add("com.spdbccc.app");

        //中国移动【有xposed检测加自我校验】
        blacklist.add("com.greenpoint.android.mc10086.activity");

        //brave浏览器【好像有个gpu的错误】
        blacklist.add("com.brave.browser");

        //下厨房，也有问题；暂不屏蔽，慢慢处理
//        blacklist.add("com.xiachufang");

        //QQ浏览器（一堆莫名的错误）
        blacklist.add("com.tencent.mtt");

        //远程桌面（基本不用，懒得处理）
        blacklist.add("com.rdc_zh.android");

        //termxui【没必要，安装在虚拟机里面】
        blacklist.add("com.termux");
    }

    public void setOnItemClickListener(ItemEventListener mItemEventListener) {
        this.mItemEventListener = mItemEventListener;
    }

    public List<AppInfo> getList() {
        return mAppList;
    }

    public void setList(List<AppInfo> models) {
        //移除黑名单app
        List<AppInfo> listCopied = new ArrayList<>();
        for(AppInfo appInfo : models){
            String pkg = appInfo.packageName;
            if(!blacklist.contains(pkg)){
                listCopied.add(appInfo);
            }
        }
        this.mAppList = listCopied;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_FOOTER) {
            return new ViewHolder(mFooterView);
        }
        return new ViewHolder(mInflater.inflate(R.layout.item_clone_app, null));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_FOOTER) {
            return;
        }
        super.onBindViewHolder(holder, position);
        AppInfo info = mAppList.get(position);

        if (mFrom == null) {
            GlideUtils.loadInstalledPackageIcon(mContext, info.packageName, holder.iconView, android.R.drawable.sym_def_app_icon);
        } else {
            GlideUtils.loadPackageIconFromApkFile(mContext, info.path, holder.iconView, android.R.drawable.sym_def_app_icon);
        }

        holder.nameView.setText(String.format("%s: %s", info.name, info.version));
        if (isIndexSelected(position)) {
            holder.iconView.setAlpha(1f);
            holder.appCheckView.setImageResource(R.drawable.ic_check);
        } else {
            holder.iconView.setAlpha(0.65f);
            holder.appCheckView.setImageResource(R.drawable.ic_no_check);
        }
        if (info.cloneCount > 0) {
            holder.labelView.setVisibility(View.VISIBLE);
            holder.labelView.setText(info.cloneCount + 1 + "");
        } else {
            holder.labelView.setVisibility(View.INVISIBLE);
        }

        if (info.path == null) {
            holder.summaryView.setVisibility(View.GONE);
        } else {
            holder.summaryView.setVisibility(View.VISIBLE);
            holder.summaryView.setText(info.path);
        }
        holder.itemView.setOnClickListener(v -> {
            mItemEventListener.onItemClick(info, position);
        });
    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
    }

    @Override
    protected boolean isIndexSelectable(int index) {
        return mItemEventListener.isSelectable(index);
    }

    @Override
    public int getItemCount() {
        return mAppList == null ? 1 : mAppList.size() + 1;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == getItemCount() - 1) {
            return TYPE_FOOTER;
        }
        return super.getItemViewType(position);
    }

    public AppInfo getItem(int index) {
        return mAppList.get(index);
    }

    public interface ItemEventListener {

        void onItemClick(AppInfo appData, int position);

        boolean isSelectable(int position);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView iconView;
        private TextView nameView;
        private ImageView appCheckView;
        private LabelView labelView;
        private TextView summaryView;

        ViewHolder(View itemView) {
            super(itemView);
            if (itemView != mFooterView) {
                iconView = (ImageView) itemView.findViewById(R.id.item_app_icon);
                nameView = (TextView) itemView.findViewById(R.id.item_app_name);
                appCheckView = (ImageView) itemView.findViewById(R.id.item_app_checked);
                labelView = (LabelView) itemView.findViewById(R.id.item_app_clone_count);
                summaryView = (TextView) itemView.findViewById(R.id.item_app_summary);
            }
        }
    }
}

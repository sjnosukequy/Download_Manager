package com.example.new_down;


import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

class RecyclerViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener, View.OnLongClickListener
{
    public TextView file_name;
    public ProgressBar progressBar;
    public TextView file_size;
    public TextView file_status;
    public Button share;
    public Button pause;
    public ImageView image;

    private ItemClickListener itemClickListener;

    public RecyclerViewHolder(View itemView) {
        super(itemView);
        file_name = (TextView)itemView.findViewById(R.id.file_title);
        progressBar = (ProgressBar) itemView.findViewById(R.id.file_progress);
        file_size = (TextView)itemView.findViewById(R.id.file_size);
        file_status = (TextView)itemView.findViewById(R.id.file_status);
        share = (Button)itemView.findViewById(R.id.sharefile);
        pause = (Button)itemView.findViewById(R.id.pause_resume);
        image = (ImageView)itemView.findViewById(R.id.image_down);

        share.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                itemClickListener.onShareClick(v, getAdapterPosition());
            }
        });

        pause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                itemClickListener.onPauseClick(v, getAdapterPosition(), String.valueOf(pause.getText()) );
            }
        });

        itemView.setOnClickListener(this);
        itemView.setOnLongClickListener(this);
    }

    public void setItemClickListener(ItemClickListener itemClickListener)
    {
        this.itemClickListener = itemClickListener;
    }

    @Override
    public void onClick(View v) {
        itemClickListener.onClick(v, getAdapterPosition(),false);
    }

    @Override
    public boolean onLongClick(View v) {
        itemClickListener.onClick(v, getAdapterPosition(),true);
        return true;
    }
}

public class RecyclerAdapter extends RecyclerView.Adapter<RecyclerViewHolder> {
    private List<DownloadModel> listData = new ArrayList<>();
    private Context context;
    private ItemClickListener listener;

    private Dictionary<String, Integer> dict = new Hashtable<>();

    public RecyclerAdapter(List<DownloadModel> listData, Context context, ItemClickListener listener) {
        this.listData = listData;
        this.context = context;
        this.listener = listener;
        dict.put("audio", R.drawable.sound);
        dict.put("image", R.drawable.photo);
        dict.put("video", R.drawable.youtube);
        dict.put("text", R.drawable.file);
        dict.put("application", R.drawable.categories);
        dict.put("font", R.drawable.font);
        dict.put("other", R.drawable.download);
    }
    @Override
    public RecyclerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View itemView = inflater.inflate(R.layout.row_item,parent,false);

        return new RecyclerViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(RecyclerViewHolder holder, int position) {
        holder.file_name.setText(listData.get(position).getTitle());
        holder.file_size.setText(listData.get(position).getFile_size());
        holder.file_status.setText(listData.get(position).getStatus());
        holder.progressBar.setProgress(Integer.parseInt(listData.get(position).getProgress()));
        holder.setItemClickListener(listener);
        String key;
        URLUtil uasd = new URLUtil();
        String url = listData.get(position).getUrl();
        String fileExtenstion = MimeTypeMap.getFileExtensionFromUrl(url);
        fileExtenstion = String.valueOf( MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileExtenstion.toLowerCase()));
        if(fileExtenstion.equalsIgnoreCase("null"))
            key = "other";
        else
        {
            String[] arrOfStr = fileExtenstion.split("/");
            if(dict.get(arrOfStr[0]) != null)
                key = arrOfStr[0];
            else
                key = "other";
        }
        Glide
                .with(this.context)
                .load(dict.get(key))
                .centerCrop()
                .into(holder.image);

        if(listData.get(position).isIs_paused()) {
            holder.pause.setText("Resume");
            holder.file_status.setText("Paused");
        }
        else
            holder.pause.setText("Paused");

        if(listData.get(position).getStatus().equalsIgnoreCase("Completed")) {
            holder.pause.setText("Delete");
            holder.pause.setBackgroundColor(Color.parseColor("#B00020"));
        }
    }

    @Override
    public int getItemCount() {
        return listData.size();
    }

}

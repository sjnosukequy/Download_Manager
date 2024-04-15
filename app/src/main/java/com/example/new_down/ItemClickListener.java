package com.example.new_down;

import android.view.View;

public interface ItemClickListener {
    public void onClick(View view,  int position,  boolean isLongClick);

    public void onPauseClick(View view, int position, String options);

    public void   onShareClick(View view, int position);
}
package com.example.new_down;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.telephony.mbms.DownloadRequest;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.Console;
import java.io.File;
import java.net.FileNameMap;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ItemClickListener {

    RecyclerView recyclerView;
    RecyclerAdapter recyclerAdapter;
    List<DownloadModel> data;
    public DatabaseHandler databaseHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        databaseHandler = new DatabaseHandler(MainActivity.this);
        data = new ArrayList<>();

//        DownloadModel test = new DownloadModel();
//        test.setFile_path("google");
//        test.setFile_size("100kb");
//        test.setProgress("30");
//        test.setStatus("Pending");
//        test.setTitle("ok");

        data = databaseHandler.getAllDown();
        if (data != null)
            if (data.size() > 0)
                for (int i = 0; i < data.size(); i++)
                    if (data.get(i).getStatus().equalsIgnoreCase("Pending") || data.get(i).getStatus().equalsIgnoreCase("Running") || data.get(i).getStatus().equalsIgnoreCase("Downloading")) {
                        DownloadStatusTask downloadStatusTask = new DownloadStatusTask(data.get(i), i);
                        runTask(downloadStatusTask, String.valueOf(data.get(i).getDownloadId()));
                    }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        }
//        databaseHandler.deleteDown(data.get(0));

        recyclerView = findViewById(R.id.Recyle);
        recyclerAdapter = new RecyclerAdapter(data, MainActivity.this, MainActivity.this);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(MainActivity.this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(recyclerAdapter);
    }

    /*END OF ONCREATE*/

    public void Clear_All(View ass) {
        //DELETE DIALOG
        AlertDialog.Builder al = new AlertDialog.Builder(MainActivity.this);
        View view2 = getLayoutInflater().inflate(R.layout.delete_dialog, null);
        TextView des = view2.findViewById(R.id.Del_text_des);
        des.setText("Do you want to clear all the download ?");
        al.setView(view2);

        al.setPositiveButton("Clear All", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Toast.makeText(MainActivity.this, "Clear All Download", Toast.LENGTH_LONG).show();
                databaseHandler.clear();
                data.clear();
                recyclerAdapter.notifyDataSetChanged();
            }
        });

        al.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        al.show();
    }

    public void showInputDialog(View ass) {
        if (!checkPermission()) {
            requestPermission();
            Toast.makeText(MainActivity.this, "Please Allow Permission to Write File", Toast.LENGTH_SHORT).show();
            return;
        }
        AlertDialog.Builder al = new AlertDialog.Builder(MainActivity.this);
        View view = getLayoutInflater().inflate(R.layout.input_dialog, null);
        al.setView(view);

        final EditText editText = view.findViewById(R.id.input);
        Button paste = view.findViewById(R.id.paste);

        paste.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                try {
                    CharSequence charSequence = clipboardManager.getPrimaryClip().getItemAt(0).getText();
                    editText.setText(charSequence);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        al.setPositiveButton("Download", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                downloadFile(String.valueOf(editText.getText()));
            }
        });

        al.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        al.show();
    }

    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            return true;
        int result = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Toast.makeText(MainActivity.this, "Please Give Permission to Upload File", Toast.LENGTH_SHORT).show();
        } else {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
        }
    }

    public void downloadFile(String url) {
        String fileExtenstion = MimeTypeMap.getFileExtensionFromUrl(url);
        String filename = URLUtil.guessFileName(url, null, fileExtenstion);
        String downloadPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
        File file = new File(downloadPath, filename);
        try {

            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url))
                    .setMimeType(fileExtenstion)
                    .setTitle(filename)
                    .setDescription("Downloading")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                    .setDestinationUri(Uri.fromFile(file))
                    .setRequiresCharging(false)
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(true);

            DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            long downloadId = downloadManager.enqueue(request);

            DownloadModel test = new DownloadModel();
            test.setDownloadId(downloadId);
            test.setFile_path("");
            test.setFile_size("0");
            test.setProgress("0");
            test.setStatus("Downloading");
            test.setTitle(filename);
            test.setIs_paused(false);
            test.setUrl(url);
            databaseHandler.addDown(test);
            DownloadModel test2 = databaseHandler.getByDownID(downloadId);
            test.setId(test2.getId());
            data.add(test);
            recyclerAdapter.notifyItemInserted(data.size() - 1);
            DownloadStatusTask downloadStatusTask = new DownloadStatusTask(test, data.size() - 1);
            runTask(downloadStatusTask, String.valueOf(downloadId));

        } catch (Exception e) {
            Toast.makeText(this, "Cannot Download The File", Toast.LENGTH_SHORT).show();
        }
    }

    public class DownloadStatusTask extends AsyncTask<String, String, String> {
        DownloadModel downloadModel;
        int index;

//        @Override
//        protected void onPreExecute() {
//            super.onPreExecute();
//            //Hàm này sẽ chạy đầu tiên khi AsyncTask này được gọi
//            //Ở đây mình sẽ thông báo quá trình load bắt đâu "Start"
//            Toast.makeText(MainActivity.this, "Start", Toast.LENGTH_SHORT).show();
//        }

        public DownloadStatusTask(DownloadModel downloadModel, int index) {
            this.downloadModel = downloadModel;
            this.index = index;
        }

        @Override
        protected String doInBackground(String... strings) {
            downloadFileProcess(strings[0]);
            return null;
        }

        @SuppressLint("Range")
        private void downloadFileProcess(String downloadId) {
            DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            boolean downloading = true;
            if (downloadModel.getStatus().equals("Completed"))
                downloading = false;
            try {
                while (downloading) {
                    DownloadManager.Query query = new DownloadManager.Query();
                    query.setFilterById(Long.parseLong(downloadId));
                    Cursor cursor = downloadManager.query(query);
                    cursor.moveToFirst();

                    @SuppressLint("Range") int bytes_downloaded = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR));
                    @SuppressLint("Range") int total_size = cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES));

                    if (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)) == DownloadManager.STATUS_SUCCESSFUL) {
                        downloading = false;
                    }

                    int progress = (int) ((bytes_downloaded * 100L) / total_size);
                    String file_size = String.valueOf(bytes_downloaded);
//                    String temp = downloadModel.getProgress();
//                    int temp2 = Integer.parseInt(temp);
                    if(  Integer.parseInt(downloadModel.getProgress()) > progress) {
                        progress = Integer.parseInt(downloadModel.getProgress());
                        Long temp = (long) progress * total_size / 100L;
                        file_size =  String.valueOf(temp);
                    }

                    NotificationCompat.Builder builder = new NotificationCompat.Builder(MainActivity.this, "download_channel")
                            .setSmallIcon(R.drawable.ic_launcher_foreground)
                            .setContentTitle("Downloading...")
                            .setContentText("Download in progress")
                            .setProgress(100, progress, false) // Set progress bar
                            .setPriority(NotificationCompat.PRIORITY_HIGH);

                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(MainActivity.this);
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {

                    }
                    else
                        notificationManager.notify(Integer.parseInt( downloadId), builder.build());

                    String status = getStatusMessage(cursor);
                    publishProgress(String.valueOf(progress), file_size, status);
                    cursor.close();
                }
            } catch (Exception e) {
                cancel(true);
            }
        }

        @Override
        protected void onProgressUpdate(final String... values) {
            super.onProgressUpdate(values);
            //DATABASE CHANGES
            downloadModel.setFile_size(bytesIntoHumanReadable(Long.parseLong(values[1])));
            downloadModel.setProgress(values[0]);
            //UI CHANGES
            data.get(index).setFile_size(bytesIntoHumanReadable(Long.parseLong(values[1])));
            data.get(index).setProgress(values[0]);
            if (!downloadModel.getStatus().equalsIgnoreCase("PAUSE") && !downloadModel.getStatus().equalsIgnoreCase("RESUME") && !downloadModel.getStatus().equalsIgnoreCase("COMPLETED")) {
                downloadModel.setStatus(values[2]);
                data.get(index).setStatus(values[2]);
            }
            databaseHandler.update(downloadModel);
            recyclerAdapter.notifyItemChanged(index);
        }
    }

    public void runTask(DownloadStatusTask downloadStatusTask, String id) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                downloadStatusTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, new String[]{id});
            } else {
                downloadStatusTask.execute(new String[]{id});
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("Range")
    private String getStatusMessage(Cursor cursor) {
        String msg = "-";
        switch (cursor.getInt(cursor.getColumnIndex(DownloadManager.COLUMN_STATUS))) {
            case DownloadManager.STATUS_FAILED:
                msg = "Failed";
                break;
            case DownloadManager.STATUS_PAUSED:
                msg = "Paused";
                break;
            case DownloadManager.STATUS_RUNNING:
                msg = "Running";
                break;
            case DownloadManager.STATUS_SUCCESSFUL:
                msg = "Completed";
                break;
            case DownloadManager.STATUS_PENDING:
                msg = "Pending";
                break;
            default:
                msg = "Unknown";
                break;
        }
        return msg;
    }

    private String bytesIntoHumanReadable(long bytes) {
        long kilobyte = 1024;
        long megabyte = kilobyte * 1024;
        long gigabyte = megabyte * 1024;
        long terabyte = gigabyte * 1024;

        if ((bytes >= 0) && (bytes < kilobyte)) {
            return bytes + " B";

        } else if ((bytes >= kilobyte) && (bytes < megabyte)) {
            return (bytes / kilobyte) + " KB";

        } else if ((bytes >= megabyte) && (bytes < gigabyte)) {
            return (bytes / megabyte) + " MB";

        } else if ((bytes >= gigabyte) && (bytes < terabyte)) {
            return (bytes / gigabyte) + " GB";

        } else if (bytes >= terabyte) {
            return (bytes / terabyte) + " TB";

        } else {
            return bytes + " Bytes";
        }
    }

    BroadcastReceiver onComplete = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            try {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                int index = -1;
                for (int i = 0; i < data.size(); i++)
                    if (data.get(i).getDownloadId() == id) {
                        index = i;
                        break;
                    }

                if (index != -1) {
                    DownloadManager.Query query = new DownloadManager.Query();
                    query.setFilterById(id);
                    DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                    Cursor cursor = downloadManager.query(new DownloadManager.Query().setFilterById(id));
                    cursor.moveToFirst();

                    @SuppressLint("Range") String downloaded_path = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                    data.get(index).setStatus("Completed");
                    Uri uri = Uri.parse(downloaded_path);
                    data.get(index).setFile_path(uri.getPath());
                    databaseHandler.update(data.get(index));
                    Toast.makeText(MainActivity.this, data.get(index).getStatus(), Toast.LENGTH_SHORT);
                    recyclerAdapter.notifyItemChanged(index);

                    NotificationCompat.Builder builder = new NotificationCompat.Builder(MainActivity.this, "download_channel")
                            .setSmallIcon(R.drawable.ic_launcher_foreground)
                            .setContentTitle("Download Complete: " + data.get(index).getTitle())
                            .setContentText("Your file has been downloaded successfully.")
                            .setPriority(NotificationCompat.PRIORITY_HIGH);

                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(MainActivity.this);
                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {

                    }
                    else
                        notificationManager.notify(Integer.parseInt(String.valueOf(id)), builder.build());

                }
            } catch (Exception e){
                e.printStackTrace();
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(onComplete);
    }

    public static void openFile(Context context, File file, String mimeType, String chooserTitle) {
        try {
            Uri contentUri = FileProvider.getUriForFile(context, context.getPackageName(), file);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(contentUri, mimeType);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // Create chooser title intent to provide a title for the chooser dialog
            Intent chooserIntent = Intent.createChooser(intent, chooserTitle);
            try {
                context.startActivity(chooserIntent);
            } catch (ActivityNotFoundException e) {
                // If no application can handle the intent, show an error message
                Toast.makeText(context, "No app found to open the file", Toast.LENGTH_SHORT).show();
            }
            Toast.makeText(context, mimeType, Toast.LENGTH_SHORT).show();

        } catch (Exception e){
            Toast.makeText(context, "Cannot open the file", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onClick(View view, int position, boolean isLongClick) {
        if (isLongClick) {
            File file = new File(data.get(position).getFile_path());
            FileNameMap fileNameMap = URLConnection.getFileNameMap();
            String fileExtenstion = fileNameMap.getContentTypeFor(file.getName());
            Toast.makeText(MainActivity.this, "Long Click: " + position + " Extension: " + fileExtenstion, Toast.LENGTH_SHORT).show();

            //DELETE DIALOG
            AlertDialog.Builder al = new AlertDialog.Builder(MainActivity.this);
            View view2 = getLayoutInflater().inflate(R.layout.delete_dialog, null);
            al.setView(view2);

            al.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                    downloadManager.remove(data.get(position).getDownloadId());
                    databaseHandler.deleteDown(data.get(position));
                    data.remove(position);
                    recyclerAdapter.notifyItemRemoved(position);
                }
            });

            al.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });

            al.show();

        } else {
            if (data.get(position).getStatus().equalsIgnoreCase("Completed")) {
                File file = new File(data.get(position).getFile_path());
                FileNameMap fileNameMap = URLConnection.getFileNameMap();
                String fileExtenstion = fileNameMap.getContentTypeFor(file.getName());
                openFile(MainActivity.this, file, fileExtenstion, "Open " + data.get(position).getTitle());
            }
        }
    }

    public void Pause_Res_Del(File file, int position, String options) {
        if (options.equalsIgnoreCase("Delete")) {
            databaseHandler.deleteDown(data.get(position));
            data.remove(position);
            recyclerAdapter.notifyItemRemoved(position);
            file.delete();
        } else if (options.equalsIgnoreCase("Resume")) {
            //RESUME FUNC
            String url = data.get(position).getUrl();
            String fileExtenstion = MimeTypeMap.getFileExtensionFromUrl(url);
            String filename = URLUtil.guessFileName(url, null, fileExtenstion);
            String downloadPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
            file = new File(downloadPath, filename);
            try {
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url))
                        .setMimeType(fileExtenstion)
                        .setTitle(filename)
                        .setDescription("Downloading")
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                        .setDestinationUri(Uri.fromFile(file))
                        .setRequiresCharging(false)
                        .setAllowedOverMetered(true)
                        .setAllowedOverRoaming(true);

                DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                long downloadId = downloadManager.enqueue(request);
                data.get(position).setDownloadId(downloadId);
                data.get(position).setIs_paused(false);
                databaseHandler.update(data.get(position));
                recyclerAdapter.notifyItemChanged(position);

                DownloadStatusTask downloadStatusTask = new DownloadStatusTask(data.get(position), position);
                runTask(downloadStatusTask, String.valueOf(downloadId));

            } catch (Exception e) {
                String stackTrace = Log.getStackTraceString(e);
                Log.d("Resume", stackTrace);
            }
        } else {
            //PAUSE FUNC
            data.get(position).setIs_paused(true);
            data.get(position).setStatus("Paused");
            databaseHandler.update(data.get(position));
            recyclerAdapter.notifyItemChanged(position);
            DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            downloadManager.remove(data.get(position).getDownloadId());
        }
    }

    @Override
    public void onPauseClick(View view, int position, String options) {
        Toast.makeText(MainActivity.this, options, Toast.LENGTH_SHORT).show();
        File file = new File(data.get(position).getFile_path());
//            FileNameMap fileNameMap = URLConnection.getFileNameMap();
//            String fileExtenstion = fileNameMap.getContentTypeFor(file.getName());
        Pause_Res_Del(file, position, options);
    }

    public static void shareFile(Context context, File file, String mimeType, String subject, String message) {
        try {
            // Generate content URI for the file using FileProvider
            Uri contentUri = FileProvider.getUriForFile(context, context.getPackageName(), file);

            // Create an intent to share the file
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType(mimeType); // Set the MIME type of the file
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri); // Attach the content URI
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, subject); // Set the subject of the message
            shareIntent.putExtra(Intent.EXTRA_TEXT, message); // Set the text message

            // Grant read permission to the receiving app
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // Launch the intent to share the file
            context.startActivity(Intent.createChooser(shareIntent, "Share File"));
        } catch (Exception e){
            Toast.makeText(context, "Cannot Share the file", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onShareClick(View view, int position) {
        if (data.get(position).getStatus().equalsIgnoreCase("Completed")) {
            File file = new File(data.get(position).getFile_path());
            FileNameMap fileNameMap = URLConnection.getFileNameMap();
            String fileExtenstion = fileNameMap.getContentTypeFor(file.getName());
            shareFile(MainActivity.this, file, fileExtenstion, data.get(position).getTitle(), fileExtenstion);
        }
    }

}
package com.impian.supremeconverter;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.JavascriptInterface;
import android.webkit.MimeTypeMap;
import android.webkit.URLUtil;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;


public class MainActivity extends AppCompatActivity {


    private WebView webView;
    public String sURL, sFileName, sUserAgent;
    private ValueCallback<Uri> mUploadMessage;
    public ValueCallback<Uri[]> uploadMessage;
    public static final int REQUEST_SELECT_FILE = 100;
    private final static int FILECHOOSER_RESULTCODE = 1;

    public String url = "https://www.supremeconverter.com/" ;
    private Object ValueCallback;
    private WebSettings webSettings;
    private String DownloadImageURL;



    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        webView = findViewById(R.id.webview);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);

        webView.addJavascriptInterface(new JavaScriptInterface(this), "Android");

        webView.loadUrl(url);
        webView.getSettings().setAllowFileAccess(true);

            //code for showing download image in notification bar
        webView.setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading (WebView view, String url) {
                if (url.endsWith(".png")) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    // if want to download pdf manually create AsyncTask here
                    // and download file
                    return true;
                }
                return false;
            }
        });


         class WebviewFragment extends Fragment {
            WebView browser;

            // invoke this method after set your WebViewClient and ChromeClient
            private void browserSettings() {
                browser.getSettings().setJavaScriptEnabled(true);
                browser.setDownloadListener(new DownloadListener() {
                    @Override
                    public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
                        browser.loadUrl(JavaScriptInterface.getBase64StringFromBlobUrl(url));
                    }
                });
                browser.getSettings().setAppCachePath(getActivity().getApplicationContext().getCacheDir().getAbsolutePath());
                browser.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
                browser.getSettings().setDatabaseEnabled(true);
                browser.getSettings().setDomStorageEnabled(true);
                browser.getSettings().setUseWideViewPort(true);
                browser.getSettings().setLoadWithOverviewMode(true);
                browser.addJavascriptInterface(new JavaScriptInterface(getContext()), "Android");
                browser.getSettings().setPluginState(WebSettings.PluginState.ON);
            }
        }


        webView.setWebChromeClient(new WebChromeClient() {
            // For 3.0+ Devices (Start)
            // onActivityResult attached before constructor
            protected void openFileChooser(ValueCallback uploadMsg, String acceptType) {
                mUploadMessage = uploadMsg;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("image/*");
                startActivityForResult(Intent.createChooser(i, "File Browser"), FILECHOOSER_RESULTCODE);
            }

            // For Lollipop 5.0+ Devices
            public boolean onShowFileChooser(WebView mWebView, ValueCallback<Uri[]> filePathCallback, WebChromeClient.FileChooserParams fileChooserParams) {
                if (uploadMessage != null) {
                    uploadMessage.onReceiveValue(null);
                    uploadMessage = null;
                }

                uploadMessage = filePathCallback;

                Intent intent = null;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    intent = fileChooserParams.createIntent();
                }
                try {
                    startActivityForResult(intent, REQUEST_SELECT_FILE);
                } catch (ActivityNotFoundException e) {
                    uploadMessage = null;
                    return false;
                }
                return true;
            }


            protected void openFileChooser(ValueCallback<Uri> uploadMsg) {
                mUploadMessage = uploadMsg;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("image/*");
                startActivityForResult(Intent.createChooser(i, "File Chooser"), FILECHOOSER_RESULTCODE);
            }
        });


        //code for download started
        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                String fileName = URLUtil.guessFileName(url, contentDisposition, getFileType(url));


                sFileName = fileName;
                sURL = url;
                sUserAgent = userAgent;

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            == PackageManager.PERMISSION_GRANTED) {
                        downloadFile(sFileName, sURL, sUserAgent);
                    } else {
                        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}
                                , 1001);
                    }
                } else {
                    downloadFile(sFileName, sURL,sUserAgent);
                }
            }
        });
   }

    private void downloadFile(String fileName, String url, String userAgent) {
        try {
            url="https://www.supremeconverter.com/";
           // DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
            String cookie = CookieManager.getInstance().getCookie(url);

            request.setTitle(fileName)
                    .setDescription("is being downloaded")
                    .addRequestHeader("cookie", cookie)
                    .addRequestHeader("User-Agent", userAgent)
                    .setMimeType(getFileType(url))
                    .setAllowedOverMetered(true)
                    .setAllowedOverRoaming(true)
                    .setVisibleInDownloadsUi (true)
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE
                                                       | DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);


            request.setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,    //Download folder
                   URLUtil.guessFileName(DownloadImageURL, null, null));  //Name of file


            DownloadManager downloadManager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);

            downloadManager.enqueue(request);

            sURL = "";
            sUserAgent = "";
            sFileName = "";
            Toast.makeText(this, "Download Started", Toast.LENGTH_LONG).show();
        } catch (Exception ignored) {
            Toast.makeText(this, "error" + ignored, Toast.LENGTH_SHORT).show();
            Log.d("@@@@", ignored.getMessage());
        }
    }


    public String getFileType(String url) {
        ContentResolver contentResolver = getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(Uri.parse(url)));
}


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (!sURL.equals("") && !sFileName.equals("") && !sUserAgent.equals("")) {
                    downloadFile(sFileName, sURL, sUserAgent);
                }
            }
        }
    }

    public static class JavaScriptInterface {
        private Context context;
        private NotificationManager nm;
        private Bitmap bitMap;



        public JavaScriptInterface(Context context) {
            this.context = context;
        }

//        @JavascriptInterface
//        public String bitMapToBase64() {
//
//            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOututStream ();
////            //add support for jpg and more.
////            bitMap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
////            byte[] byteArray = byteArrayOutputStream .toByteArray();
////            String encoded = Base64.encodeToString(byteArray, Base64.DEFAULT);
////            return encoded;p
//        }
//
//        public static Bitmap decodeBase64(String str) {
//            byte[] decodedByte = Base64.decode(str, Base64.DEFAULT);
//            return BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.length);
//
////            image.setImageBitmap(decodedByte);
//
//        }

        @JavascriptInterface
        public void getBase64FromBlobData(String base64Data) throws IOException {
            convertBase64StringToPdfAndStoreIt(base64Data);
        }

        public static String getBase64StringFromBlobUrl(String blobUrl) {
            if (blobUrl.startsWith("blob")) {

                return "javascript: var xhr=new XMLHttpRequest();" +
                        "xhr.open('GET', '" + blobUrl + "', true);" +
                        "xhr.setRequestHeader('Content-type','application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;charset=UTF-8');" +
                        "xhr.responseType = 'blob';" +
                        "xhr.onload = function(e) {" +
                        "    if (this.status == 200) {" +
                        "        var blobPdf = this.response;" +
                        "        var reader = new FileReader();" +
                        "        reader.readAsDataURL(blobPdf);" +
                        "        reader.unloaded = function() {" +
                        "            base64data = reader.result;" +
                        "            Android.getBase64FromBlobData(base64data);" +
                        "        }" +
                        "    }" +
                        "};" +
                        "xhr.send();";
            }
            return "javascript: console.log('It is not a Blob URL');";
        }

        private void convertBase64StringToPdfAndStoreIt(String base64PDf) throws IOException {

            Log.e("base64PDf", base64PDf);
            String currentDateTime = DateFormat.getDateTimeInstance().format(new Date());
            Calendar calendar = Calendar.getInstance();
            ;
            final File dwldsPath = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS) + "/Report" + calendar.getTimeInMillis() + "_.xlsx");
            byte[] pdfAsBytes = Base64.decode(base64PDf.replaceFirst("data:application/vnd.openxmlformats-officedocument.spreadsheetml.sheet;base64,", ""), 0);
            Log.e("bytearray", "" + pdfAsBytes);
            FileOutputStream os;
            os = new FileOutputStream(dwldsPath, false);
            os.write(pdfAsBytes);
            os.flush();
            os.close();

            if (dwldsPath.exists()) {
                sendNotification();


                File dir = new File(Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS) + "/Report" +
                        calendar.getTimeInMillis() + "_.xlsx");
                Intent sendIntent = new Intent(Intent.ACTION_VIEW);
                String path = dir.getAbsolutePath();

                Uri uri;
                if (Build.VERSION.SDK_INT < 24) {
                    uri = Uri.fromFile(dir);
                } else {
                    File file = new File(path);
                    uri = FileProvider.getUriForFile((MainActivity) this.context,
                            this.context.getApplicationContext().getPackageName() + ".provider", file);
//                    uri = Uri.parse("file://" + dir);
                }
                sendIntent.setDataAndType(uri, "application/vnd.ms-excel");
                sendIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                sendIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                try {
                    context.startActivity(sendIntent);

                } catch (Exception e) {
                    Toast.makeText(context, "Np app found to view file", Toast.LENGTH_SHORT).show();
                }
            }
        }

        private void sendNotification() {
        }
    }






    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (requestCode == REQUEST_SELECT_FILE) {
                if (uploadMessage == null)
                    return;
                uploadMessage.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, intent));
                uploadMessage = null;
            }
        } else if (requestCode == FILECHOOSER_RESULTCODE) {
            if (null == mUploadMessage)
                return;
            // Use MainActivity.RESULT_OK if you're implementing WebView inside Fragment
            // Use RESULT_OK only if you're implementing WebView inside an Activity
            Uri result = intent == null || resultCode != MainActivity.RESULT_OK ? null : intent.getData();
            mUploadMessage.onReceiveValue(result);
            mUploadMessage = null;
        }


    }

    private static class xWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }

        return super.onKeyDown(keyCode, event);
    }
}
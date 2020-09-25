package org.vwork.android.aida64remote;

import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;

import androidx.appcompat.app.AppCompatActivity;

import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {
    private static final String WEB_URL = "http://VILLAIN-PC:4897";
    private HealthCheckThread checkThread;
    private boolean startFailed = false;
    private WebView webView;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.webView = (WebView) findViewById(R.id.webView);
        this.webView.getSettings().setJavaScriptEnabled(true);
        this.webView.getSettings().setLoadWithOverviewMode(true);
        this.webView.getSettings().setUseWideViewPort(true);
    }

    protected void onResume() {
        super.onResume();
        System.out.println("onResume");
        this.webView.onResume();
        this.webView.resumeTimers();
        start();
    }

    protected void onPause() {
        super.onPause();
        System.out.println("onPause");
        this.webView.onPause();
        this.webView.pauseTimers();
        stop();
    }

    private void start() {
        new Thread() {
            public void run() {
                if (MainActivity.this.checkHealth()) {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            MainActivity.this.startFailed = false;
                            MainActivity.this.webView.setVisibility(View.VISIBLE);
                            MainActivity.this.webView.loadUrl(WEB_URL);
                        }
                    });
                } else {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            MainActivity.this.startFailed = true;
                            MainActivity.this.webView.setVisibility(View.INVISIBLE);
                        }
                    });
                }
                MainActivity.this.checkThread = new HealthCheckThread();
                MainActivity.this.checkThread.start();
            }
        }.start();
    }

    private void stop() {
        this.webView.setVisibility(View.INVISIBLE);
        if (this.checkThread != null) {
            this.checkThread.release();
            this.checkThread = null;
        }
    }

    private class HealthCheckThread extends Thread {
        private boolean running;

        private HealthCheckThread() {
            this.running = true;
        }

        public void release() {
            this.running = false;
        }

        public void run() {
            while (this.running) {
                if (MainActivity.this.checkHealth()) {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            if (MainActivity.this.startFailed) {
                                MainActivity.this.startFailed = false;
                                MainActivity.this.webView.setVisibility(View.VISIBLE);
                                MainActivity.this.webView.loadUrl(WEB_URL);
                            }
                            MainActivity.this.webView.setKeepScreenOn(true);
                        }
                    });
                } else {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            MainActivity.this.webView.setKeepScreenOn(false);
                        }
                    });
                }
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean checkHealth() {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) new URL(WEB_URL).openConnection();
            connection.setConnectTimeout(1000);
            connection.setReadTimeout(1000);
            int code = connection.getResponseCode();
            if (code < 200 || code > 300) {
                throw new RuntimeException("http status code is " + code);
            }
            System.out.println("check health success");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("check health failed:" + e.getMessage());
            return false;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
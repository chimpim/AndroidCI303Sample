package com.chimpim.ci303sample;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.chimpim.reader.ci303.CI303Reader;
import com.chimpim.reader.ci303.CI303ReaderSupport;
import com.chimpim.reader.ci303.CI303Reader_AndroidSerialPortImpl;
import com.chimpim.reader.ci303.TagData;

import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends Activity {
    private Button mBtnConnect;
    private Button mBtnStartRead;
    private Button mBtnStopRead;
    private TextView mTvLog;
    private CI303Reader mCI303Reader;

    private AtomicBoolean isStartRead = new AtomicBoolean(false);

    private static final ExecutorService IO_THREAD_POOL = Executors.newCachedThreadPool();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        mCI303Reader = new CI303Reader_AndroidSerialPortImpl();
        mBtnConnect.setOnClickListener(v -> runOnIoThread(() -> {
            // 自动检测串口号
            String connect = CI303ReaderSupport.connect(mCI303Reader);
            runOnUiThread(() -> {
                if (TextUtils.isEmpty(connect)) {
                    Toast.makeText(this, "检测不到CI303读卡器", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "连接成功(" + connect + ")", Toast.LENGTH_SHORT).show();
                }
            });

        }));
        mBtnStartRead.setOnClickListener(v -> runOnIoThread(() -> {
            isStartRead.set(true);
            while (isStartRead.get()) {
                try {
                    TagData[] tagData = CI303ReaderSupport.gen2MultiTagIdentify(mCI303Reader, 1000);
                    runOnUiThread(() -> mTvLog.setText(Arrays.toString(tagData) + "\n" + mTvLog.getText()));
                } catch (IOException e) {
                    e.printStackTrace();
                    runOnUiThread(() -> mTvLog.setText("读卡异常:" + e.toString() + "\n" + mTvLog.getText()));
                }
            }
        }));
        mBtnStopRead.setOnClickListener(v -> isStartRead.set(false));
    }

    private void initView() {
        mBtnConnect = (Button) findViewById(R.id.btn_connect);
        mBtnStartRead = (Button) findViewById(R.id.btn_start_read);
        mBtnStopRead = (Button) findViewById(R.id.btn_stop_read);
        mTvLog = (TextView) findViewById(R.id.tv_log);
    }

    private void runOnIoThread(Runnable action) {
        if (action == null) throw new NullPointerException("action no null");
        IO_THREAD_POOL.execute(action);
    }
}

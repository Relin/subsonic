package net.sourceforge.subsonic.android;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Button;
import net.sourceforge.subsonic.android.domain.MusicDirectory;
import net.sourceforge.subsonic.android.service.MusicService;
import net.sourceforge.subsonic.android.service.MusicServiceFactory;
import net.sourceforge.subsonic.android.util.BackgroundTask;

import java.util.List;

public class SelectAlbumActivity extends Activity implements AdapterView.OnItemClickListener, AdapterView.OnItemSelectedListener {

    private static final String TAG = SelectAlbumActivity.class.getSimpleName();
    private final DownloadServiceConnection downloadServiceConnection = new DownloadServiceConnection();
    private DownloadService downloadService;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_album);
        final ListView albumList = (ListView) findViewById(R.id.select_album_albums);
        final ListView songList = (ListView) findViewById(R.id.select_album_songs);
        albumList.setOnItemClickListener(SelectAlbumActivity.this);
        songList.setOnItemSelectedListener(SelectAlbumActivity.this);
        songList.setItemsCanFocus(false);

        bindService(new Intent(this, DownloadService.class),
                    downloadServiceConnection, Context.BIND_AUTO_CREATE);
        BackgroundTask<MusicDirectory> task = new BackgroundTask<MusicDirectory>(this) {
            @Override
            protected MusicDirectory doInBackground() throws Throwable {
                MusicService musicService = MusicServiceFactory.getMusicService();
                String path = getIntent().getStringExtra(Constants.NAME_PATH);
                return musicService.getMusicDirectory(path, this);
            }

            @Override
            protected void done(MusicDirectory result) {
                List<MusicDirectory.Entry> albums = result.getChildren(true, false);
                List<MusicDirectory.Entry> songs = result.getChildren(false, true);
                albumList.setAdapter(new ArrayAdapter<MusicDirectory.Entry>(SelectAlbumActivity.this, android.R.layout.simple_list_item_1, albums));
                songList.setAdapter(new ArrayAdapter<MusicDirectory.Entry>(SelectAlbumActivity.this, android.R.layout.simple_list_item_multiple_choice, songs));
            }

            @Override
            protected void cancel() {
                MusicServiceFactory.getMusicService().cancel(this);
                finish();
            }
        };
        task.execute();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(downloadServiceConnection);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (position >= 0) {
            MusicDirectory.Entry entry = (MusicDirectory.Entry) parent.getItemAtPosition(position);
            Log.d(TAG, entry + " clicked.");
            if (entry.isDirectory()) {
                Intent intent = new Intent(this, SelectAlbumActivity.class);
                intent.putExtra(Constants.NAME_PATH, entry.getPath());
                startActivity(intent);
            } else {
                download(entry);
            }
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
        setDownloadButtonEnabled(true);
    }

    @Override
    public void onNothingSelected(AdapterView<?> adapterView) {
        setDownloadButtonEnabled(false);
    }

    private void setDownloadButtonEnabled(boolean enabled) {
        Button button = (Button) findViewById(R.id.select_album_download);
        button.setEnabled(enabled);
    }

    private void download(MusicDirectory.Entry entry) {
        try {
            if (downloadService != null) {
                downloadService.download(entry);
            } else {
                Log.e(TAG, "Not connected to Download Service.");
            }

        } catch (Exception e) {
            Log.e(TAG, "Failed to contact Download Service.");
        }
    }


    private class DownloadServiceConnection implements ServiceConnection {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            downloadService = ((DownloadService.DownloadBinder) service).getService();
            Log.i(TAG, "Connected to Download Service");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            downloadService = null;
            Log.i(TAG, "Disconnected from Download Service");
        }
    }

}
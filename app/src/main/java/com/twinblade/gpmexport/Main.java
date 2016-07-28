package com.twinblade.gpmexport;

import android.Manifest;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

public class Main extends AppCompatActivity {

    private static String[] LIST_IDS = {"1", "2", "3", "4"};

    private ArrayList<Song> mSongs;
    private ArrayList<ListItem> mListItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
            return;
        }

        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    mSongs = parseSongs();
                    mListItems = parseListItems();

                    for (String id : LIST_IDS) {
                        findListedSongs(id);
                    }

                    findUnlistedSongs();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }

    private void findUnlistedSongs() throws Exception {
        FileOutputStream fos = new FileOutputStream(new File(Environment.getExternalStorageDirectory(), "unlisted.csv"));
        fos.write("Artist,Title\n".getBytes());

        int listedCount = 0;
        int unlistedCount = 0;
        for (Song song : mSongs) {
            boolean listed = false;
            for (ListItem item : mListItems) {
                if (TextUtils.equals(item.mSongId, song.mId)) {
                    listed = true;
                    listedCount++;
                    break;
                }
            }

            if (!listed && !song.mArtist.equals("Stuff You Should Know")) {
                String title = song.mTitle;
                // Log.e("GPME", title);

                String line = title + "," + song.mArtist + "\n";
                fos.write(line.getBytes());

                unlistedCount++;
            }
        }

        Log.e("GPME", "Listed: " + listedCount + "; Unlisted: " + unlistedCount);

        fos.flush();
        fos.close();
    }

    private void findListedSongs(String listId) throws Exception {
        FileOutputStream fos = new FileOutputStream(new File(Environment.getExternalStorageDirectory(), "pl" + listId + ".csv"));
        fos.write("Artist,Title\n".getBytes());

        int count = 0;
        for (ListItem item : mListItems) {
            if (item.mListId.equals(listId)) {
                for (Song song : mSongs) {
                    if (song.mId.equals(item.mSongId)) {
                        String title = song.mTitle;
                        // Log.e("GPME", title);

                        String line = title + "," + song.mArtist + "\n";
                        fos.write(line.getBytes());

                        count++;
                        break;
                    }
                }
            }
        }

        fos.flush();
        fos.close();

        Log.e("GPME", "Playlist " + listId + ": " + count);
    }

    private ArrayList<Song> parseSongs() throws Exception {
        File dbFile = new File(Environment.getExternalStorageDirectory(), "music.db");
        SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(dbFile, null);
        ArrayList<Song> songs = new ArrayList<>();

        Cursor c = db.query("MUSIC", new String[] {"Id", "Title", "Artist"}, null, null, null, null, null);
        c.moveToFirst();
        while (!c.isLast()) {
            Song song = new Song();
            song.mId = c.getString(c.getColumnIndex("Id"));
            song.mTitle = c.getString(c.getColumnIndex("Title"));
            song.mArtist = c.getString(c.getColumnIndex("Artist"));
            songs.add(song);

            c.moveToNext();
        }

        c.close();
        return songs;
    }

    private ArrayList<ListItem> parseListItems() throws Exception {
        File dbFile = new File(Environment.getExternalStorageDirectory(), "music.db");
        SQLiteDatabase db = SQLiteDatabase.openOrCreateDatabase(dbFile, null);
        ArrayList<ListItem> items = new ArrayList<>();

        Cursor c = db.query("ListItems", new String[] {"MusicId", "ListId"}, null, null, null, null, null);
        c.moveToFirst();
        while (!c.isLast()) {
            ListItem item = new ListItem();
            item.mListId = c.getString(c.getColumnIndex("ListId"));
            item.mSongId = c.getString(c.getColumnIndex("MusicId"));
            items.add(item);

            c.moveToNext();
        }

        c.close();
        return items;
    }

    private class Song {

        public String mTitle;
        public String mArtist;
        public String mId;
    }

    private class ListItem {

        public String mListId;
        public String mSongId;
    }
}

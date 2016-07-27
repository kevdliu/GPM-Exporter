package com.twinblade.gpmexport;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.File;
import java.io.FileInputStream;
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
        FileOutputStream fos = new FileOutputStream(new File(Environment.getExternalStorageDirectory(), "unlisted.xml"));

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

            if (!listed) {
                if (!song.mArtist.equals("Stuff You Should Know")) {

                    String title = song.mTitle;
                    Log.e("GPME", title);

                    String line = title + " - " + song.mArtist + "\n";
                    fos.write(line.getBytes());

                    unlistedCount++;
                }
            }
        }

        Log.e("GPME", "Listed: " + listedCount + "; Unlisted: " + unlistedCount);

        fos.flush();
        fos.close();
    }

    private void findListedSongs(String listId) throws Exception {
        FileOutputStream fos = new FileOutputStream(new File(Environment.getExternalStorageDirectory(), "k" + listId + ".xml"));

        int count = 0;
        for (ListItem item : mListItems) {
            if (item.mListId.equals(listId)) {
                for (Song song : mSongs) {
                    if (song.mId.equals(item.mSongId)) {
                        String title = song.mTitle;
                        Log.e("GPME", title);

                        String line = title + " - " + song.mArtist + "\n";
                        fos.write(line.getBytes());

                        count++;
                        break;
                    }
                }
            }
        }

        fos.flush();
        fos.close();

        Log.e("GPME", "Playlist K" + listId + ": " + count);
    }

    private ArrayList<Song> parseSongs() throws Exception {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        XmlPullParser parser = factory.newPullParser();

        FileInputStream fis = new FileInputStream(new File(Environment.getExternalStorageDirectory(), "music.xml"));
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        parser.setInput(fis, null);

        ArrayList<Song> songs = new ArrayList<>();
        int eventType = parser.getEventType();
        Song currentSong = null;

        while (eventType != XmlPullParser.END_DOCUMENT) {
            String tag;
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    tag = parser.getName();

                    if (TextUtils.equals(tag, "row")) {
                        currentSong = new Song();
                    } else if (currentSong != null) {
                        if (TextUtils.equals(tag, "Title")) {
                            currentSong.mTitle = parser.nextText();
                        } else if (TextUtils.equals(tag, "Artist")) {
                            currentSong.mArtist = parser.nextText();
                        } else if (TextUtils.equals(tag, "Id")) {
                            currentSong.mId = parser.nextText();
                        }
                    }
                    break;
                case XmlPullParser.END_TAG:
                    tag = parser.getName();
                    if (TextUtils.equals(tag, "row") && currentSong != null){
                        songs.add(currentSong);
                    }
            }

            eventType = parser.next();
        }

        fis.close();
        return songs;
    }

    private ArrayList<ListItem> parseListItems() throws Exception {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        XmlPullParser parser = factory.newPullParser();

        FileInputStream fis = new FileInputStream(new File(Environment.getExternalStorageDirectory(), "listitems.xml"));
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        parser.setInput(fis, null);

        ArrayList<ListItem> items = new ArrayList<>();
        int eventType = parser.getEventType();
        ListItem currentItem = null;

        while (eventType != XmlPullParser.END_DOCUMENT) {
            String tag;
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    tag = parser.getName();

                    if (TextUtils.equals(tag, "row")) {
                        currentItem = new ListItem();
                    } else if (currentItem != null) {
                        if (TextUtils.equals(tag, "MusicId")) {
                            currentItem.mSongId = parser.nextText();
                        } else if (TextUtils.equals(tag, "ListId")) {
                            currentItem.mListId = parser.nextText();
                        }
                    }
                    break;
                case XmlPullParser.END_TAG:
                    tag = parser.getName();
                    if (TextUtils.equals(tag, "row") && currentItem != null){
                        items.add(currentItem);
                    }
            }

            eventType = parser.next();
        }

        fis.close();
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

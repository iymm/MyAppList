package com.projectsexception.myapplist;

import java.io.File;
import java.util.ArrayList;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.text.Html;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.projectsexception.myapplist.fragments.ShareFragment;
import com.projectsexception.myapplist.fragments.ShareTaskFragment;
import com.projectsexception.myapplist.model.AppInfo;
import com.projectsexception.myapplist.util.AppUtil;
import com.projectsexception.myapplist.util.CustomLog;

public class ShareActivity extends BaseActivity implements 
        ActionBar.TabListener, ShareFragment.CallBack, ShareTaskFragment.CallBack {
    
    private static final String TAG_TASK_FRAGMENT = "task_fragment";
    
    public static final int SECTION_XML = 0;
    public static final int SECTION_HTML = 1;
    public static final int SECTION_TEXT = 2;

    public static final String APP_LIST = "app_list";
    public static final String FILE_PATH = "file_path";
    
    SectionsPagerAdapter mSectionsPagerAdapter;
    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;
    
    /**
     * Store the XML file if exists
     */
    File mFile;
    
    /**
     * Store the selected apps to share
     */
    ArrayList<AppInfo> mAppList;
    
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);

        // Set up the action bar.
        final ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle(R.string.ab_title_share);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        
        // If we receive the file path, we can share xml
        String filePath = getIntent().getStringExtra(FILE_PATH);
        if (filePath != null) {
            mFile = new File(filePath);
        }
        mAppList = getIntent().getParcelableArrayListExtra(APP_LIST);

        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager(), filePath != null);

        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });
        
        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
            // Create a tab with text corresponding to the page title defined by
            // the adapter. Also specify this Activity object, which implements
            // the TabListener interface, as the callback (listener) for when
            // this tab is selected.
            actionBar.addTab(actionBar.newTab().setText(mSectionsPagerAdapter.getPageTitle(i)).setTabListener(this));
        }
        
        FragmentManager fm = getSupportFragmentManager();
        ShareTaskFragment frg = (ShareTaskFragment) fm.findFragmentByTag(TAG_TASK_FRAGMENT);
        if (frg == null) {
            frg = new ShareTaskFragment();
            fm.beginTransaction().add(frg, TAG_TASK_FRAGMENT).commit();
        } else {
            frg.checkPending();
        }
    }

    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in
        // the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    class SectionsPagerAdapter extends FragmentPagerAdapter {
        
        private boolean xmlAvailable;

        public SectionsPagerAdapter(FragmentManager fm, boolean xmlAvailable) {
            super(fm);
            this.xmlAvailable = xmlAvailable;
        }

        @Override
        public Fragment getItem(int position) {
            int realPosition = xmlAvailable ? position : position + 1;
            int section;
            if (realPosition == 0) {
                section = SECTION_XML;
            } else if (realPosition == 1) {
                section = SECTION_HTML;
            } else {
                section = SECTION_TEXT;
            }
            return ShareFragment.newInstance(section);
        }

        @Override
        public int getCount() {
            int count = 2;
            if (xmlAvailable) {
                count++;
            }
            return count;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            int realPosition = xmlAvailable ? position : position + 1;
            if (realPosition == 0) {
                return getString(R.string.share_tab_xml);
            } else if (realPosition == 1) {
                return getString(R.string.share_tab_html);
            } else if (realPosition == 2) {
                return getString(R.string.share_tab_text);
            }
            return null;
        }
    }

    @Override
    public void shareAppList(int section, boolean file, boolean footer) {
        if (file) {
            if (section == SECTION_XML) {
                saveFinished(section, mFile);
            } else {
                FragmentManager fm = getSupportFragmentManager();
                ShareTaskFragment frg = (ShareTaskFragment) fm.findFragmentByTag(TAG_TASK_FRAGMENT);
                if (frg != null) {
                    frg.startTask(section, mAppList);
                }
            }
        } else {
            shareAppListText(section, footer);
        }
    }
    
    @Override
    public void saveFinished(int section, File file) {
        if (file != null && file.exists()) {
            Intent intent = new Intent(android.content.Intent.ACTION_SEND);
            if (section == SECTION_XML) {
                intent.setType("text/xml");                
            } else if (section == SECTION_HTML) {
                intent.setType("text/html");                
            } else {
                intent.setType("text/plain");
            }
            intent.putExtra(android.content.Intent.EXTRA_SUBJECT, 
                    getResources().getQuantityString(R.plurals.share_title, 2)); 
            intent.putExtra(android.content.Intent.EXTRA_TEXT, 
                    getString(R.string.share_file_text, getPackageName())); 
            intent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://"+ file.getAbsolutePath()));
            try {
                startActivity(Intent.createChooser(intent, getString(R.string.share_chooser)));
            } catch (Exception e) {
                CustomLog.error("ShareActivity", e);
                Toast.makeText(this, getString(R.string.share_file_send_failed, file.getAbsolutePath()), Toast.LENGTH_LONG).show();                    
            }
            finish();
        } else {
            Toast.makeText(this, getString(R.string.share_file_failed, file.getAbsolutePath()), Toast.LENGTH_LONG).show();                    
        }
    }
    
    private void shareAppListText(int section, boolean footer) {
        if (mAppList != null && !mAppList.isEmpty()) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_SUBJECT, 
                    getResources().getQuantityString(R.plurals.share_title, mAppList.size()));
            final CharSequence text;
            if (section == SECTION_HTML) {
                intent.setType("text/html");
                text = Html.fromHtml(AppUtil.appInfoToHTML(this, mAppList, footer));
            } else {
                intent.setType("text/plain");                    
                text = AppUtil.appInfoToText(this, mAppList, footer);
            }
            intent.putExtra(Intent.EXTRA_TEXT, text);
            try {
                startActivity(Intent.createChooser(intent, getString(R.string.share_chooser)));                
            } catch (Exception e) {
                CustomLog.error("ShareActivity", e);
                Toast.makeText(this, getString(R.string.share_text_send_failed), Toast.LENGTH_SHORT).show();
            }
            finish();
        }
    }

}

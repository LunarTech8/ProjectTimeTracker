package com.romanbrunner.apps.projecttimetracker;

import android.app.AlarmManager;
import android.content.Context;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.LineChart;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;
import com.romanbrunner.apps.projecttimetracker.data.DailyTimePoolRepository;
import com.romanbrunner.apps.projecttimetracker.data.TimeEntryRepository;
import com.romanbrunner.apps.projecttimetracker.util.PreferencesManager;

/**
 * Main activity for time tracking.
 */
public class MainActivity extends AppCompatActivity
{
    // Constants:
    private static final int SECTION_PADDING_DP = 24;
    private static final int SECTION_MARGIN_DP = 8;
    private static final int VISIBLE_SECTION_COLUMNS = 3;

    // UI Components:
    private RecyclerView rvSectionSelector;
    private MaterialCardView cardControlPanel;
    private MaterialCardView cardEntries;
    private MaterialCardView cardOverview;
    private MaterialCardView cardPools;
    private SectionSelectorAdapter sectionSelectorAdapter;
    private int selectedSectionIndex = 0;
    private Button btnLoadEntries;
    private Button btnSaveEntries;
    private TextView tvTimeRangeLabelMain;
    private Button btnLoadPoolsMain;
    private Button btnSavePoolsMain;
    private Button btnRemoveCategoryMain;
    private Button btnAddPoolMain;

    // Managers:
    private ControlPanelManager controlPanelManager;
    private TimeEntriesManager entriesManager;
    private TimePoolsManager poolsManager;
    private TimeOverviewManager chartManager;

    // File pickers:
    private ActivityResultLauncher<String[]> loadEntriesFileLauncher;
    private ActivityResultLauncher<String> saveEntriesFileLauncher;
    private ActivityResultLauncher<String[]> loadPoolsFileLauncher;
    private ActivityResultLauncher<String> savePoolsFileLauncher;

    // Data:
    private TimeEntryRepository timeEntryRepository;
    private DailyTimePoolRepository dailyTimePoolRepository;
    private PreferencesManager preferencesManager;
    private AlarmManager alarmManager;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initializeFilePickers();
        timeEntryRepository = new TimeEntryRepository(this);
        dailyTimePoolRepository = new DailyTimePoolRepository(this);
        preferencesManager = new PreferencesManager(this);
        alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        initializeViews();
        initializeManagers();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        controlPanelManager.onDestroy();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        controlPanelManager.onResume();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        controlPanelManager.onPause();
    }

    private void initializeViews()
    {
        rvSectionSelector = findViewById(R.id.rv_section_selector);
        cardControlPanel = findViewById(R.id.card_control_panel);
        cardEntries = findViewById(R.id.card_entries);
        cardOverview = findViewById(R.id.card_overview);
        cardPools = findViewById(R.id.card_pools);
        btnLoadEntries = findViewById(R.id.btn_load_entries);
        btnSaveEntries = findViewById(R.id.btn_save_entries);
        tvTimeRangeLabelMain = findViewById(R.id.tv_time_range_label_main);
        btnLoadPoolsMain = findViewById(R.id.btn_load_pools_main);
        btnSavePoolsMain = findViewById(R.id.btn_save_pools_main);
        btnRemoveCategoryMain = findViewById(R.id.btn_remove_category_main);
        btnAddPoolMain = findViewById(R.id.btn_add_pool_main);
        setupSectionSelector();
    }

    private void initializeManagers()
    {
        // Control Panel Manager:
        controlPanelManager = new ControlPanelManager(
            this,
            timeEntryRepository,
            dailyTimePoolRepository,
            preferencesManager,
            alarmManager,
            findViewById(R.id.btn_start_stop),
            findViewById(R.id.btn_reset),
            findViewById(R.id.btn_end),
            findViewById(R.id.spinner_reminder),
            findViewById(R.id.spinner_project),
            findViewById(R.id.spinner_category),
            findViewById(R.id.tv_current_duration),
            findViewById(R.id.tv_total_project_duration),
            findViewById(R.id.tv_total_category_duration),
            findViewById(R.id.tv_pool_time),
            findViewById(R.id.tv_start_date)
        );
        controlPanelManager.setOnControlPanelEventListener(new ControlPanelManager.OnControlPanelEventListener()
        {
            @Override
            public void onEntryEnded()
            {
                entriesManager.refreshEntryList();
            }

            @Override
            public void onTimerStateChanged()
            {
                // Timer state changed, could refresh UI if needed
            }
        });
        controlPanelManager.initialize();
        // Entries Manager:
        entriesManager = new TimeEntriesManager(
            this,
            findViewById(R.id.rv_entries),
            timeEntryRepository
        );
        entriesManager.setOnEntriesChangedListener(() ->
        {
            controlPanelManager.updateSpinnerData();
            controlPanelManager.updateTotalDurations();
            controlPanelManager.updatePoolTime();
        });
        entriesManager.setupRecyclerView();
        btnLoadEntries.setOnClickListener(v -> loadEntriesFileLauncher.launch(new String[]{"text/plain"}));
        btnSaveEntries.setOnClickListener(v -> saveEntriesFileLauncher.launch("MetaDataProjectTime.txt"));
        // Pools Manager:
        RecyclerView rvPoolsMain = findViewById(R.id.rv_pools_main);
        TextView tvPoolResetInterval = findViewById(R.id.tv_pool_reset_interval);
        poolsManager = new TimePoolsManager(this, rvPoolsMain, dailyTimePoolRepository, timeEntryRepository, preferencesManager, tvPoolResetInterval);
        poolsManager.setupRecyclerView();
        controlPanelManager.setPoolsManager(poolsManager);
        btnLoadPoolsMain.setOnClickListener(v -> loadPoolsFileLauncher.launch(new String[]{"text/plain"}));
        btnSavePoolsMain.setOnClickListener(v -> savePoolsFileLauncher.launch("MetaDataDailyTimePools.txt"));
        btnRemoveCategoryMain.setOnClickListener(v -> poolsManager.showRemoveCategoryDialog());
        btnAddPoolMain.setOnClickListener(v -> poolsManager.showAddCategoryDialog());
        // Chart Manager:
        LineChart chartMain = findViewById(R.id.chart_main);
        ImageButton btnTimePrevMain = findViewById(R.id.btn_time_prev_main);
        ImageButton btnTimeNextMain = findViewById(R.id.btn_time_next_main);
        chartManager = new TimeOverviewManager(this, chartMain, timeEntryRepository, btnTimePrevMain, btnTimeNextMain, tvTimeRangeLabelMain);
        chartManager.setupChart();
        chartManager.setupClickListeners();
        initializePoolsFilePickers();
    }

    private void initializeFilePickers()
    {
        loadEntriesFileLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> { if (uri != null) entriesManager.loadEntriesFromFile(uri); }
        );
        saveEntriesFileLauncher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("text/plain"),
                uri -> { if (uri != null) entriesManager.saveEntriesToFile(uri); }
        );
    }

    private void initializePoolsFilePickers()
    {
        loadPoolsFileLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> { if (uri != null) poolsManager.loadPoolsFromFile(uri); }
        );
        savePoolsFileLauncher = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("text/plain"),
                uri -> { if (uri != null) poolsManager.savePoolsToFile(uri); }
        );
    }

    private void setupSectionSelector()
    {
        String[] sections = {
            getString(R.string.control_header),
            getString(R.string.entries_header),
            getString(R.string.pools_header),
            getString(R.string.overview_header)
        };
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;
        int paddingPx = (int)(SECTION_PADDING_DP * displayMetrics.density);
        int marginPerItemPx = (int)(SECTION_MARGIN_DP * displayMetrics.density);
        int availableWidth = screenWidth - paddingPx;
        int itemWidth = (availableWidth - (marginPerItemPx * VISIBLE_SECTION_COLUMNS)) / VISIBLE_SECTION_COLUMNS;
        sectionSelectorAdapter = new SectionSelectorAdapter(sections, itemWidth);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rvSectionSelector.setLayoutManager(layoutManager);
        rvSectionSelector.setAdapter(sectionSelectorAdapter);
    }

    private void showSection(int index)
    {
        selectedSectionIndex = index;
        sectionSelectorAdapter.notifyDataSetChanged();
        cardControlPanel.setVisibility(index == 0 ? View.VISIBLE : View.GONE);
        cardEntries.setVisibility(index == 1 ? View.VISIBLE : View.GONE);
        cardPools.setVisibility(index == 2 ? View.VISIBLE : View.GONE);
        cardOverview.setVisibility(index == 3 ? View.VISIBLE : View.GONE);
        if (index == 2)
        {
            poolsManager.refreshPoolsData();
        }
        if (index == 3)
        {
            chartManager.loadChartData(true);
        }
    }

    private class SectionSelectorAdapter extends RecyclerView.Adapter<SectionSelectorAdapter.ViewHolder>
    {
        private final String[] sections;
        private final int itemWidth;

        SectionSelectorAdapter(String[] sections, int itemWidth)
        {
            this.sections = sections;
            this.itemWidth = itemWidth;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
        {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_section_selector, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position)
        {
            String section = sections[position];
            holder.textView.setText(section);
            ViewGroup.LayoutParams layoutParams = holder.itemView.getLayoutParams();
            layoutParams.width = itemWidth;
            holder.itemView.setLayoutParams(layoutParams);
            boolean isSelected = position == selectedSectionIndex;
            int backgroundColor = isSelected ? getResources().getColor(R.color.section_selected, null) : getResources().getColor(R.color.section_unselected, null);
            holder.card.setCardBackgroundColor(backgroundColor);
            holder.itemView.setOnClickListener(v -> showSection(position));
        }

        @Override
        public int getItemCount()
        {
            return sections.length;
        }

        class ViewHolder extends RecyclerView.ViewHolder
        {
            MaterialCardView card;
            TextView textView;

            ViewHolder(View itemView)
            {
                super(itemView);
                card = itemView.findViewById(R.id.section_card);
                textView = itemView.findViewById(R.id.section_text);
            }
        }
    }
}

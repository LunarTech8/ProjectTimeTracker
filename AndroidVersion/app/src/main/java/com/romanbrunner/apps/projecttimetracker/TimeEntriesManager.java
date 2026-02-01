package com.romanbrunner.apps.projecttimetracker;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.romanbrunner.apps.projecttimetracker.data.TimeEntryRepository;
import com.romanbrunner.apps.projecttimetracker.model.TimeEntry;
import com.romanbrunner.apps.projecttimetracker.util.TimeUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manager class for time entries functionality.
 */
public class TimeEntriesManager
{
    /**
     * Callback interface for entry list changes.
     */
    public interface OnEntriesChangedListener
    {
        void onEntriesChanged();
    }

    private final Context context;
    private final RecyclerView rvEntries;
    private final TimeEntryRepository timeEntryRepository;
    private TimeEntryAdapter adapter;
    private OnEntriesChangedListener listener;

    public TimeEntriesManager(Context context, RecyclerView rvEntries, TimeEntryRepository timeEntryRepository)
    {
        this.context = context;
        this.rvEntries = rvEntries;
        this.timeEntryRepository = timeEntryRepository;
    }

    public void setOnEntriesChangedListener(OnEntriesChangedListener listener)
    {
        this.listener = listener;
    }

    public void setupRecyclerView()
    {
        adapter = new TimeEntryAdapter(timeEntryRepository.getAllEntries());
        rvEntries.setLayoutManager(new LinearLayoutManager(context));
        rvEntries.setAdapter(adapter);
    }

    public void refreshEntryList()
    {
        if (adapter != null)
        {
            adapter.updateEntries(timeEntryRepository.getAllEntries());
        }
    }

    public void loadEntriesFromFile(Uri uri)
    {
        try
        {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream != null)
            {
                timeEntryRepository.importFromTextFile(inputStream);
                inputStream.close();
                refreshEntryList();
                notifyEntriesChanged();
                Toast.makeText(context, "Entries loaded successfully", Toast.LENGTH_SHORT).show();
            }
        }
        catch (Exception e)
        {
            Toast.makeText(context, "Error loading file: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    public void saveEntriesToFile(Uri uri)
    {
        try
        {
            OutputStream outputStream = context.getContentResolver().openOutputStream(uri);
            if (outputStream != null)
            {
                timeEntryRepository.exportToTextFile(outputStream);
                outputStream.close();
                Toast.makeText(context, "Entries saved successfully", Toast.LENGTH_SHORT).show();
            }
        }
        catch (Exception e)
        {
            Toast.makeText(context, "Error saving file: " + e.getMessage(), Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void notifyEntriesChanged()
    {
        if (listener != null)
        {
            listener.onEntriesChanged();
        }
    }

    private class TimeEntryAdapter extends RecyclerView.Adapter<TimeEntryAdapter.ViewHolder>
    {
        private List<TimeEntry> entries;

        TimeEntryAdapter(List<TimeEntry> entries)
        {
            this.entries = new ArrayList<>(entries);
            // Reverse to show newest first:
            Collections.reverse(this.entries);
        }

        void updateEntries(List<TimeEntry> newEntries)
        {
            this.entries = new ArrayList<>(newEntries);
            Collections.reverse(this.entries);
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
        {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_time_entry, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position)
        {
            TimeEntry entry = entries.get(position);
            holder.tvProject.setText(entry.getProject());
            holder.tvCategory.setText(entry.getCategory());
            holder.tvDuration.setText(TimeUtils.formatDuration(entry.getDurationSeconds()));
            holder.tvStartTime.setText(TimeUtils.formatDateTimeForDisplay(entry.getStartTime()));
            holder.btnRemove.setOnClickListener(v ->
            {
                new AlertDialog.Builder(context)
                        .setTitle(R.string.confirm_delete)
                        .setMessage(R.string.confirm_delete_message)
                        .setPositiveButton(R.string.delete, (dialog, which) ->
                        {
                            // Find actual index in repository (entries are reversed):
                            int actualIndex = timeEntryRepository.getEntryCount() - 1 - position;
                            timeEntryRepository.removeEntry(actualIndex);
                            refreshEntryList();
                            notifyEntriesChanged();
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            });
        }

        @Override
        public int getItemCount()
        {
            return entries.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder
        {
            TextView tvProject, tvCategory, tvDuration, tvStartTime;
            ImageButton btnRemove;

            ViewHolder(View itemView)
            {
                super(itemView);
                tvProject = itemView.findViewById(R.id.tv_entry_project);
                tvCategory = itemView.findViewById(R.id.tv_entry_category);
                tvDuration = itemView.findViewById(R.id.tv_entry_duration);
                tvStartTime = itemView.findViewById(R.id.tv_entry_start_time);
                btnRemove = itemView.findViewById(R.id.btn_remove_entry);
            }
        }
    }
}

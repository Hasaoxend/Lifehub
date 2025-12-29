package com.test.lifehub.features.two_productivity.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.test.lifehub.R;
import com.test.lifehub.features.two_productivity.data.NoteEntry;

import java.util.ArrayList;
import java.util.List;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class NotesListActivity extends AppCompatActivity {

    private ProductivityViewModel mViewModel;
    private NoteAdapter mNoteAdapter;
    private RecyclerView mRecyclerView;
    private FloatingActionButton mFab;
    private Toolbar mToolbar;
    private SearchView mSearchView;
    private TextView mEmptyView;

    private List<NoteEntry> mAllNotes = new ArrayList<>();

    private final ActivityResultLauncher<Intent> noteActivityResultLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    // Không cần làm gì, LiveData tự cập nhật
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notes_list);

        mToolbar = findViewById(R.id.toolbar_notes_list);
        mRecyclerView = findViewById(R.id.recycler_view_notes_list);
        mFab = findViewById(R.id.fab_add_note);
        mSearchView = findViewById(R.id.search_view_notes);
        mEmptyView = findViewById(R.id.empty_view_notes);

        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle(R.string.title_notes);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        mToolbar.setNavigationOnClickListener(v -> finish());

        mNoteAdapter = new NoteAdapter(this, noteActivityResultLauncher);
        mRecyclerView.setAdapter(mNoteAdapter);
        StaggeredGridLayoutManager layoutManager =
                new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
        mRecyclerView.setLayoutManager(layoutManager);

        mFab.setOnClickListener(v -> {
            Intent intent = new Intent(NotesListActivity.this, AddEditNoteActivity.class);
            noteActivityResultLauncher.launch(intent);
        });

        setupSearchView();

        mViewModel = new ViewModelProvider(this).get(ProductivityViewModel.class);

        mViewModel.getAllNotes().observe(this, notes -> {
            mAllNotes = notes != null ? notes : new ArrayList<>();
            filterNotes(mSearchView.getQuery().toString());
        });
    }

    private void setupSearchView() {
        mSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterNotes(newText);
                return true;
            }
        });
    }

    private void filterNotes(String query) {
        List<NoteEntry> filteredList = new ArrayList<>();

        if (query == null || query.trim().isEmpty()) {
            filteredList = mAllNotes;
        } else {
            String lowerCaseQuery = query.toLowerCase().trim();
            for (NoteEntry note : mAllNotes) {
                if (note.getTitle().toLowerCase().contains(lowerCaseQuery) ||
                        note.getContent().toLowerCase().contains(lowerCaseQuery)) {
                    filteredList.add(note);
                }
            }
        }

        mNoteAdapter.submitList(filteredList);
        updateEmptyView(filteredList);
    }

    private void updateEmptyView(List<NoteEntry> notes) {
        if (notes == null || notes.isEmpty()) {
            mRecyclerView.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.VISIBLE);
        } else {
            mRecyclerView.setVisibility(View.VISIBLE);
            mEmptyView.setVisibility(View.GONE);
        }
    }
}
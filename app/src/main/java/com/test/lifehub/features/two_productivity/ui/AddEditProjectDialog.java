package com.test.lifehub.features.two_productivity.ui;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.test.lifehub.R;
import com.test.lifehub.features.two_productivity.data.ProjectEntry;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * Dialog (Hộp thoại) để Thêm/Sửa một Thư mục (Project)
 */
@AndroidEntryPoint
public class AddEditProjectDialog extends DialogFragment {

    private static final String TAG = "AddEditProjectDialog";

    // Chìa khóa cho Bundle
    private static final String ARG_PROJECT_ID = "project_id";
    private static final String ARG_PROJECT_NAME = "project_name";
    private static final String ARG_PARENT_PROJECT_ID = "parent_project_id";

    private TextInputEditText mEtProjectName;
    private TextView mTvTitle;

    private ProductivityViewModel mViewModel;
    private String mCurrentProjectId = null;
    private String mCurrentProjectName = null;
    private String mParentProjectId = null;

    /**
     * Dùng khi Sửa (Edit) một project
     */
    public static AddEditProjectDialog newInstance(@Nullable ProjectEntry project) {
        return newInstance(project, null);
    }

    /**
     * Dùng khi Tạo (Create) một project mới
     * @param parentProjectId ID của thư mục cha (null nếu là root)
     */
    public static AddEditProjectDialog newInstance(@Nullable ProjectEntry project, @Nullable String parentProjectId) {
        AddEditProjectDialog dialog = new AddEditProjectDialog();
        Bundle args = new Bundle();
        if (project != null) {
            args.putString(ARG_PROJECT_ID, project.documentId);
            args.putString(ARG_PROJECT_NAME, project.getName());
        }
        if (parentProjectId != null) {
            args.putString(ARG_PARENT_PROJECT_ID, parentProjectId);
        }
        dialog.setArguments(args);
        return dialog;
    }

    public AddEditProjectDialog() {
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_add_edit_project, null);

        mEtProjectName = view.findViewById(R.id.et_project_name);
        mTvTitle = view.findViewById(R.id.tv_dialog_project_title);

        // Lấy ViewModel của Activity
        mViewModel = new ViewModelProvider(requireActivity()).get(ProductivityViewModel.class);

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setView(view)
                .setPositiveButton(R.string.save, null)
                .setNegativeButton(R.string.cancel, (dialog, which) -> dismiss());

        if (getArguments() != null) {
            mCurrentProjectId = getArguments().getString(ARG_PROJECT_ID);
            mCurrentProjectName = getArguments().getString(ARG_PROJECT_NAME);
            mParentProjectId = getArguments().getString(ARG_PARENT_PROJECT_ID);
        }

        if (mCurrentProjectId != null) {
            mTvTitle.setText(R.string.project_rename);
            mEtProjectName.setText(mCurrentProjectName);
        } else {
            mTvTitle.setText(R.string.project_new);
        }

        Log.d(TAG, "Dialog created. ParentID: " + mParentProjectId);

        AlertDialog dialog = builder.create();

        dialog.setOnShowListener(d -> {
            Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> saveProject());
        });

        return dialog;
    }

    private void saveProject() {
        String projectName = mEtProjectName.getText().toString().trim();

        if (TextUtils.isEmpty(projectName)) {
            Toast.makeText(getContext(), R.string.project_name_required, Toast.LENGTH_SHORT).show();
            return;
        }

        if (mCurrentProjectId == null) {
            // ----- THÊM MỚI -----
            mViewModel.insertProject(projectName, mParentProjectId); // Truyền cả parentId
            Toast.makeText(getContext(), R.string.project_created, Toast.LENGTH_SHORT).show();

        } else {
            // ----- CẬP NHẬT (SỬA) -----
            mViewModel.updateProjectName(mCurrentProjectId, projectName);
            Toast.makeText(getContext(), R.string.project_updated, Toast.LENGTH_SHORT).show();
        }

        dismiss();
    }
}
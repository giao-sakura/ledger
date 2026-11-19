package com.example.ledger.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;

import com.example.ledger.R;
import com.example.ledger.data.DiaryEntry;
import com.example.ledger.databinding.SheetDiaryBinding;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public class DiarySheet extends BottomSheetDialogFragment {
    public static final String REQUEST = "ledger.diary";
    public static final String EXTRA_ACTION = "action";
    public static final String EXTRA_ID = "id";
    public static final String EXTRA_BODY = "body";
    public static final String EXTRA_TIME = "time";
    public static final String ACTION_SAVE = "save";
    public static final String ACTION_DELETE = "delete";
    private static final String EXTRA_YEAR = "year";
    private static final String EXTRA_MONTH = "month";
    private static final String EXTRA_DAY = "day";

    private SheetDiaryBinding binding;
    private long id;
    private long originalAt;
    private LocalDate selectedDate = LocalDate.now();

    public static void open(FragmentManager manager, @Nullable DiaryEntry entry, LocalDate suggested) {
        DiarySheet sheet = new DiarySheet();
        Bundle args = new Bundle();
        LocalDate date = suggested == null ? LocalDate.now() : suggested;
        if (entry != null) {
            args.putLong(EXTRA_ID, entry.id);
            args.putString(EXTRA_BODY, entry.body);
            args.putLong(EXTRA_TIME, entry.occurredAt);
            date = Instant.ofEpochMilli(entry.occurredAt).atZone(ZoneId.systemDefault()).toLocalDate();
        }
        args.putInt(EXTRA_YEAR, date.getYear());
        args.putInt(EXTRA_MONTH, date.getMonthValue());
        args.putInt(EXTRA_DAY, date.getDayOfMonth());
        sheet.setArguments(args);
        sheet.show(manager, "diary");
    }

    @Override
    public int getTheme() {
        return R.style.Theme_Ledger_BottomSheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = SheetDiaryBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Bundle args = getArguments() == null ? new Bundle() : getArguments();
        id = args.getLong(EXTRA_ID);
        originalAt = args.getLong(EXTRA_TIME);
        int year = args.getInt(EXTRA_YEAR, LocalDate.now().getYear());
        int month = args.getInt(EXTRA_MONTH, LocalDate.now().getMonthValue());
        int day = args.getInt(EXTRA_DAY, LocalDate.now().getDayOfMonth());
        selectedDate = LocalDate.of(year, month, day);
        binding.title.setText(id > 0 ? R.string.edit_diary : R.string.new_diary);
        binding.deleteButton.setVisibility(id > 0 ? View.VISIBLE : View.GONE);
        String body = args.getString(EXTRA_BODY);
        if (body != null) {
            binding.body.setText(body);
            binding.body.setSelection(binding.body.getText().length());
        }
        renderDate();
        binding.dateRow.setOnClickListener(v -> pickDate());
        binding.saveButton.setOnClickListener(v -> save());
        binding.deleteButton.setOnClickListener(v -> confirmDelete());
        binding.body.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                binding.bodyError.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() instanceof BottomSheetDialog) {
            BottomSheetDialog dialog = (BottomSheetDialog) getDialog();
            FrameLayout bottom = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottom != null) {
                BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottom);
                behavior.setSkipCollapsed(true);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            }
        }
    }

    @Override
    public void onDestroyView() {
        binding = null;
        super.onDestroyView();
    }

    private void renderDate() {
        if (binding == null) {
            return;
        }
        binding.dateLabel.setText(getString(
                R.string.year_month_day,
                selectedDate.getYear(),
                selectedDate.getMonthValue(),
                selectedDate.getDayOfMonth()
        ));
    }

    private void pickDate() {
        DatePickerDialog dialog = new DatePickerDialog(
                requireContext(),
                (view, year, month, day) -> {
                    selectedDate = LocalDate.of(year, month + 1, day);
                    renderDate();
                },
                selectedDate.getYear(),
                selectedDate.getMonthValue() - 1,
                selectedDate.getDayOfMonth()
        );
        dialog.show();
    }

    private void save() {
        if (binding == null) {
            return;
        }
        String text = binding.body.getText().toString().trim();
        if (text.isEmpty()) {
            binding.bodyError.setVisibility(View.VISIBLE);
            binding.body.requestFocus();
            return;
        }
        Bundle result = new Bundle();
        result.putString(EXTRA_ACTION, ACTION_SAVE);
        result.putLong(EXTRA_ID, id);
        result.putString(EXTRA_BODY, text);
        result.putLong(EXTRA_TIME, resolveTime());
        getParentFragmentManager().setFragmentResult(REQUEST, result);
        dismiss();
    }

    private long resolveTime() {
        ZoneId zone = ZoneId.systemDefault();
        if (originalAt > 0) {
            LocalDate originalDay = Instant.ofEpochMilli(originalAt).atZone(zone).toLocalDate();
            if (originalDay.equals(selectedDate)) {
                return originalAt;
            }
        }
        if (selectedDate.equals(LocalDate.now(zone))) {
            return System.currentTimeMillis();
        }
        return selectedDate.atTime(12, 0).atZone(zone).toInstant().toEpochMilli();
    }

    private void confirmDelete() {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.delete_diary)
                .setMessage(R.string.delete_diary_confirm)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    Bundle result = new Bundle();
                    result.putString(EXTRA_ACTION, ACTION_DELETE);
                    result.putLong(EXTRA_ID, id);
                    getParentFragmentManager().setFragmentResult(REQUEST, result);
                    dismiss();
                })
                .show();
    }
}

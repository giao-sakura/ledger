package com.example.ledger.ui;

import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.lifecycle.ViewModelProvider;

import com.example.ledger.R;
import com.example.ledger.data.Category;
import com.example.ledger.data.Money;
import com.example.ledger.data.Transaction;
import com.example.ledger.databinding.SheetEditorBinding;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.chip.Chip;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EditorSheet extends BottomSheetDialogFragment {
    public static final String REQUEST = "ledger.editor";
    public static final String EXTRA_ACTION = "action";
    public static final String EXTRA_ID = "id";
    public static final String EXTRA_TYPE = "type";
    public static final String EXTRA_AMOUNT = "amount";
    public static final String EXTRA_CATEGORY = "category";
    public static final String EXTRA_NOTE = "note";
    public static final String EXTRA_TIME = "time";
    public static final String ACTION_SAVE = "save";
    public static final String ACTION_DELETE = "delete";

    private SheetEditorBinding binding;
    private final Map<Integer, String> keyByChipId = new HashMap<>();
    private LedgerViewModel ledgerViewModel;
    private List<Category> catalog = new ArrayList<>();
    private boolean chipsReady;
    private long id;
    private long originalAt;
    private int currentType = Transaction.EXPENSE;
    private String selectedKey;
    private LocalDate selectedDate = LocalDate.now();

    public static void open(androidx.fragment.app.FragmentManager manager, @Nullable Transaction transaction) {
        EditorSheet sheet = new EditorSheet();
        Bundle args = new Bundle();
        if (transaction != null) {
            args.putLong(EXTRA_ID, transaction.id);
            args.putInt(EXTRA_TYPE, transaction.type);
            args.putLong(EXTRA_AMOUNT, transaction.amountCents);
            args.putString(EXTRA_CATEGORY, transaction.categoryKey);
            args.putString(EXTRA_NOTE, transaction.note);
            args.putLong(EXTRA_TIME, transaction.occurredAt);
        }
        sheet.setArguments(args);
        sheet.show(manager, "editor");
    }

    @Override
    public int getTheme() {
        return R.style.Theme_Ledger_BottomSheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = SheetEditorBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        Bundle args = getArguments() == null ? new Bundle() : getArguments();
        id = args.getLong(EXTRA_ID);
        originalAt = args.getLong(EXTRA_TIME);
        int initialType = args.getInt(EXTRA_TYPE, Transaction.EXPENSE);
        if (initialType != Transaction.INCOME) {
            initialType = Transaction.EXPENSE;
        }
        String initialCategory = args.getString(EXTRA_CATEGORY);
        ZoneId zone = ZoneId.systemDefault();
        selectedDate = originalAt > 0
                ? Instant.ofEpochMilli(originalAt).atZone(zone).toLocalDate()
                : LocalDate.now(zone);

        binding.title.setText(id > 0 ? R.string.edit_entry : R.string.new_entry);
        binding.deleteButton.setVisibility(id > 0 ? View.VISIBLE : View.GONE);
        binding.amount.setFilters(new InputFilter[]{new MoneyInputFilter()});
        long amount = args.getLong(EXTRA_AMOUNT);
        if (amount > 0) {
            binding.amount.setText(Money.plain(amount));
            binding.amount.setSelection(binding.amount.getText().length());
        }
        String note = args.getString(EXTRA_NOTE);
        if (note != null) {
            binding.note.setText(note);
        }
        binding.amount.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (binding != null) {
                    binding.amountError.setVisibility(View.GONE);
                }
            }
        });

        currentType = initialType;
        selectedKey = initialCategory;
        binding.typeToggle.check(initialType == Transaction.INCOME ? R.id.typeIncome : R.id.typeExpense);
        tintAmount(currentType);
        renderDate();
        binding.typeToggle.addOnButtonCheckedListener((group, checkedId, isChecked) -> {
            if (!isChecked || binding == null) {
                return;
            }
            int type = checkedId == R.id.typeIncome ? Transaction.INCOME : Transaction.EXPENSE;
            if (type == currentType) {
                return;
            }
            currentType = type;
            tintAmount(type);
            bindCategories(type, null);
        });
        binding.categories.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) {
                return;
            }
            String key = keyByChipId.get(checkedIds.get(0));
            if (key != null) {
                selectedKey = key;
            }
        });
        ledgerViewModel = new ViewModelProvider(requireActivity()).get(LedgerViewModel.class);
        ledgerViewModel.getCategories().observe(getViewLifecycleOwner(), list -> {
            catalog = list == null ? new ArrayList<>() : list;
            if (!chipsReady && binding != null) {
                chipsReady = true;
                bindCategories(currentType, selectedKey);
            }
        });
        binding.dateRow.setOnClickListener(v -> pickDate());
        binding.saveButton.setOnClickListener(v -> save());
        binding.deleteButton.setOnClickListener(v -> confirmDelete());

        if (id == 0) {
            binding.amount.post(this::showAmountKeyboard);
        }
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

    private void showAmountKeyboard() {
        if (binding == null) {
            return;
        }
        binding.amount.requestFocus();
        InputMethodManager manager = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        if (manager != null) {
            manager.showSoftInput(binding.amount, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void tintAmount(int type) {
        if (binding == null) {
            return;
        }
        int color = requireContext().getColor(type == Transaction.INCOME ? R.color.income : R.color.expense);
        binding.amount.setTextColor(color);
        binding.currency.setTextColor(color);
    }

    private void bindCategories(int type, @Nullable String preferredKey) {
        if (binding == null) {
            return;
        }
        binding.categories.removeAllViews();
        keyByChipId.clear();
        List<Category> categories = Category.ofType(catalog, type);
        String key = preferredKey;
        boolean found = false;
        if (key != null) {
            for (int i = 0; i < categories.size(); i++) {
                if (categories.get(i).key.equals(key)) {
                    found = true;
                    break;
                }
            }
        }
        if (!found) {
            key = categories.isEmpty() ? null : categories.get(0).key;
        }
        selectedKey = key;
        for (int i = 0; i < categories.size(); i++) {
            Category category = categories.get(i);
            Chip chip = new Chip(requireContext(), null, com.google.android.material.R.attr.chipStyle);
            chip.setId(View.generateViewId());
            chip.setText(category.name);
            chip.setCheckable(true);
            chip.setCheckedIconVisible(false);
            chip.setEnsureMinTouchTargetSize(false);
            keyByChipId.put(chip.getId(), category.key);
            binding.categories.addView(chip);
            if (category.key.equals(selectedKey)) {
                chip.setChecked(true);
            }
        }
        Chip addChip = new Chip(requireContext(), null, com.google.android.material.R.attr.chipStyle);
        addChip.setText(R.string.new_category);
        addChip.setCheckable(false);
        addChip.setCheckedIconVisible(false);
        addChip.setEnsureMinTouchTargetSize(false);
        addChip.setOnClickListener(v -> promptNewCategory());
        binding.categories.addView(addChip);
    }

    private void promptNewCategory() {
        if (binding == null || ledgerViewModel == null) {
            return;
        }
        View form = getLayoutInflater().inflate(R.layout.dialog_category, null);
        EditText input = form.findViewById(R.id.categoryName);
        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(currentType == Transaction.INCOME
                        ? R.string.new_income_category
                        : R.string.new_expense_category)
                .setView(form)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.add, null)
                .create();
        dialog.setOnShowListener(shown -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            Category created = ledgerViewModel.addCategory(currentType, input.getText().toString());
            if (created == null) {
                input.setError(getString(R.string.category_name_error));
                return;
            }
            catalog = ledgerViewModel.currentCategories();
            selectedKey = created.key;
            bindCategories(currentType, created.key);
            dialog.dismiss();
        }));
        dialog.show();
        input.requestFocus();
    }

    private void renderDate() {
        if (binding == null) {
            return;
        }
        if (selectedDate.getYear() == LocalDate.now().getYear()) {
            binding.dateLabel.setText(getString(R.string.month_day, selectedDate.getMonthValue(), selectedDate.getDayOfMonth()));
        } else {
            binding.dateLabel.setText(getString(
                    R.string.year_month_day,
                    selectedDate.getYear(),
                    selectedDate.getMonthValue(),
                    selectedDate.getDayOfMonth()
            ));
        }
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
        long cents = Money.parseYuan(binding.amount.getText().toString());
        if (cents < 0 || selectedKey == null) {
            binding.amountError.setVisibility(View.VISIBLE);
            binding.amount.requestFocus();
            return;
        }
        Bundle result = new Bundle();
        result.putString(EXTRA_ACTION, ACTION_SAVE);
        result.putLong(EXTRA_ID, id);
        result.putInt(EXTRA_TYPE, currentType);
        result.putLong(EXTRA_AMOUNT, cents);
        result.putString(EXTRA_CATEGORY, selectedKey);
        result.putString(EXTRA_NOTE, binding.note.getText().toString().trim());
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
                .setTitle(R.string.delete_entry)
                .setMessage(R.string.delete_confirm)
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

    private static final class MoneyInputFilter implements InputFilter {
        @Override
        public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {
            StringBuilder next = new StringBuilder();
            next.append(dest.subSequence(0, dstart));
            next.append(source.subSequence(start, end));
            next.append(dest.subSequence(dend, dest.length()));
            if (next.length() == 0) {
                return null;
            }
            if (next.toString().matches("\\d{0,8}(\\.\\d{0,2})?")) {
                return null;
            }
            return "";
        }
    }
}

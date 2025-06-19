package com.fpt.attendix.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.fpt.attendix.R;
import com.fpt.attendix.data.repository.Result;
import com.fpt.attendix.ui.main.MainActivity;
import com.google.android.material.textfield.TextInputEditText;

public class RegisterFragment extends Fragment {

    private AuthViewModel authViewModel;
    private TextInputEditText editTextStudentId, editTextEmail, editTextPassword;
    private Button buttonRegister;
    private ProgressBar progressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        editTextStudentId = view.findViewById(R.id.editTextStudentId);
        editTextEmail = view.findViewById(R.id.editTextEmailRegister);
        editTextPassword = view.findViewById(R.id.editTextPasswordRegister);
        buttonRegister = view.findViewById(R.id.buttonRegister);
        progressBar = view.findViewById(R.id.progressBarRegister);

        setupObservers();
        setupClickListeners();
    }

    private void setupObservers() {
        authViewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            buttonRegister.setEnabled(!isLoading);
        });

        authViewModel.registerResult.observe(getViewLifecycleOwner(), result -> {
            if (result instanceof Result.Success) {
                Toast.makeText(getContext(), "Registration Successful", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getActivity(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                if (getActivity() != null) {
                    getActivity().finish();
                }
            } else if (result instanceof Result.Error) {
                Exception e = ((Result.Error<Void>) result).getError();
                Toast.makeText(getContext(), "Registration Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupClickListeners() {
        buttonRegister.setOnClickListener(v -> {
            Editable studentIdEditable = editTextStudentId.getText();
            String studentId = studentIdEditable != null ? studentIdEditable.toString().trim() : "";

            Editable emailEditable = editTextEmail.getText();
            String email = emailEditable != null ? emailEditable.toString().trim() : "";

            Editable passwordEditable = editTextPassword.getText();
            String password = passwordEditable != null ? passwordEditable.toString().trim() : "";

            if (studentId.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            if (password.length() < 6) {
                Toast.makeText(getContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
                return;
            }

            authViewModel.register(studentId, email, password);
        });
    }
}

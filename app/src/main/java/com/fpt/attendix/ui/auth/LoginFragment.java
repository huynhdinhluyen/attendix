package com.fpt.attendix.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.fpt.attendix.R;
import com.fpt.attendix.data.repository.Result;
import com.fpt.attendix.ui.main.MainActivity;
import com.google.android.material.textfield.TextInputEditText;

public class LoginFragment extends Fragment {

    private AuthViewModel authViewModel;
    private TextInputEditText editTextEmail, editTextPassword;
    private Button buttonLogin;
    private ProgressBar progressBar;
    private TextView textViewGoToRegister;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        editTextEmail = view.findViewById(R.id.editTextEmail);
        editTextPassword = view.findViewById(R.id.editTextPassword);
        buttonLogin = view.findViewById(R.id.buttonLogin);
        progressBar = view.findViewById(R.id.progressBarLogin);
        textViewGoToRegister = view.findViewById(R.id.textViewGoToRegister);

        setupObservers();
        setupClickListeners();
    }

    private void setupObservers() {
        authViewModel.isLoading.observe(getViewLifecycleOwner(), isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            buttonLogin.setEnabled(!isLoading);
        });

        authViewModel.loginResult.observe(getViewLifecycleOwner(), result -> {
            if (result instanceof Result.Success) {
                Toast.makeText(getContext(), "Login Successful", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(getActivity(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                if (getActivity() != null) {
                    getActivity().finish();
                }
            } else if (result instanceof Result.Error) {
                Exception e = ((Result.Error<Void>) result).getError();
                Toast.makeText(getContext(), "Login Failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupClickListeners() {
        buttonLogin.setOnClickListener(v -> {
            Editable emailEditable = editTextEmail.getText();
            String email = emailEditable != null ? emailEditable.toString().trim() : "";

            Editable passwordEditable = editTextPassword.getText();
            String password = passwordEditable != null ? passwordEditable.toString().trim() : "";

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(getContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }
            authViewModel.login(email, password);
        });

        textViewGoToRegister.setOnClickListener(v -> getParentFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new RegisterFragment())
                .addToBackStack(null)
                .commit());
    }
}

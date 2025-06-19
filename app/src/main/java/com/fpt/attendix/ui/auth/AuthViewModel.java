package com.fpt.attendix.ui.auth;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.fpt.attendix.data.model.User;
import com.fpt.attendix.data.repository.AttendanceRepository;
import com.fpt.attendix.data.repository.Result;

public class AuthViewModel extends ViewModel {

    private final AttendanceRepository authRepository;
    private final MutableLiveData<Result<Void>> _loginResult = new MutableLiveData<>();
    public final LiveData<Result<Void>> loginResult = _loginResult;

    private final MutableLiveData<Result<Void>> _registerResult = new MutableLiveData<>();
    public final LiveData<Result<Void>> registerResult = _registerResult;

    private final MutableLiveData<Boolean> _isLoading = new MutableLiveData<>(false);
    public final LiveData<Boolean> isLoading = _isLoading;

    public AuthViewModel() {
        this.authRepository = AttendanceRepository.getInstance();
    }

    public void login(String email, String password) {
        _isLoading.setValue(true);
        authRepository.loginUser(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                _loginResult.setValue(new Result.Success<>(null));
            } else {
                _loginResult.setValue(new Result.Error<>(task.getException()));
            }
            _isLoading.setValue(false);
        });
    }

    public void register(String studentId, String email, String password) {
        _isLoading.setValue(true);
        authRepository.registerUser(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null && task.getResult().getUser() != null) {
                String uid = task.getResult().getUser().getUid();
                User newUser = new User(uid, studentId, email);
                saveUserDetails(newUser);
            } else {
                _registerResult.setValue(new Result.Error<>(task.getException()));
                _isLoading.setValue(false);
            }
        });
    }

    private void saveUserDetails(User user) {
        authRepository.saveUserDetails(user).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                _registerResult.setValue(new Result.Success<>(null));
            } else {
                _registerResult.setValue(new Result.Error<>(task.getException()));
            }
            _isLoading.setValue(false);
        });
    }
}

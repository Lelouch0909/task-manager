package com.taskmanager.api.auth.unit;
import com.taskmanager.api.auth.service.*;
import com.taskmanager.api.auth.service.impl.AuthServiceImpl;
import com.taskmanager.api.auth.dto.request.*;
import com.taskmanager.api.auth.dto.response.*;
import com.taskmanager.api.auth.model.CodePurpose;
import com.taskmanager.api.notification.service.EmailService;
import com.taskmanager.api.common.exception.ApiException;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import java.time.Instant;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.*;

class AuthServiceTest {
    @Test void emailFailureHappensAfterStateWasSaved() {
        var state = mock(AuthStateService.class);
        var emails = mock(EmailService.class);
        var service = new AuthServiceImpl(state, emails);
        var request = new RegisterRequest("Alice", "alice@example.com", "Password1!");
        var user = new UserResponse(UUID.randomUUID(), "Alice", request.email(), false, Instant.now());
        when(state.register(request)).thenReturn(new AuthStateService.CodeDispatch(user, "123456", CodePurpose.VERIFY_EMAIL));
        doThrow(new ApiException(503, "email_unavailable", "Unavailable")).when(emails).sendVerification(user.email(), "123456");
        assertThatThrownBy(() -> service.register(request)).isInstanceOf(ApiException.class);
        var order = inOrder(state, emails);
        order.verify(state).register(request);
        order.verify(emails).sendVerification(user.email(), "123456");
    }
    @Test void unknownEmailDoesNotSendAnything() {
        var state = mock(AuthStateService.class);
        var emails = mock(EmailService.class);
        new AuthServiceImpl(state, emails).forgot(new EmailRequest("missing@example.com"));
        verifyNoInteractions(emails);
    }
}

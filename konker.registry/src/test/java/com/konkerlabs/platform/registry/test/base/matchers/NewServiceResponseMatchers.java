package com.konkerlabs.platform.registry.test.base.matchers;

import com.konkerlabs.platform.registry.business.services.api.NewServiceResponse;
import com.konkerlabs.platform.registry.business.services.api.ServiceResponse;
import com.konkerlabs.platform.registry.business.services.api.ServiceResponseBuilder;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class NewServiceResponseMatchers {

    private static abstract class BaseMatcher<ServiceResponse> extends TypeSafeMatcher<ServiceResponse> {

        protected void describeMismatchSafely(ServiceResponse item, Description mismatchDescription) {
            mismatchDescription.appendText("was ").appendValue(item);
        }
    }

    public static Matcher<NewServiceResponse> isResponseOk() {
        return new BaseMatcher<NewServiceResponse>() {
            @Override
            protected boolean matchesSafely(NewServiceResponse item) {
                return item.getStatus().equals(NewServiceResponse.Status.OK) &&
                       item.getResponseMessages().isEmpty();
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(
                    ServiceResponseBuilder.ok().build().toString()
                );
            }
        };
    }

    public static Matcher<NewServiceResponse> hasErrorMessage(String code, Object... parameters) {
        return new BaseMatcher<NewServiceResponse>() {
            @Override
            protected boolean matchesSafely(NewServiceResponse item) {
                return item.getStatus().equals(NewServiceResponse.Status.ERROR) &&
                       item.getResponseMessages().containsKey(code);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(
                    ServiceResponseBuilder.error()
                        .withMessage(code, parameters)
                        .build().toString()
                );
            }
        };
    }

    public static Matcher<NewServiceResponse> hasAllErrors(Map<String, Object[]> messages) {
        return new BaseMatcher<NewServiceResponse>() {
            @Override
            protected boolean matchesSafely(NewServiceResponse item) {
                return item.getStatus().equals(NewServiceResponse.Status.ERROR) &&
                        item.getResponseMessages().equals(messages);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(
                        ServiceResponseBuilder.error()
                                .withMessages(messages)
                                .build().toString()
                );
            }
        };
    }
}

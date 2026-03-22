package com.dna_testing_system.dev.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApplicationContextHolderTest {

    @Mock
    private ApplicationContext mockContext;

    private ApplicationContextHolder holder;

    @BeforeEach
    void setUp() {
        holder = new ApplicationContextHolder();
    }

    @Test
    void setApplicationContext_ShouldStoreContext_WhenCalled() {
        // Act
        holder.setApplicationContext(mockContext);

        // Assert: getBean phải hoạt động sau khi set context
        SomeService mockBean = mock(SomeService.class);
        when(mockContext.getBean(SomeService.class)).thenReturn(mockBean);

        SomeService result = ApplicationContextHolder.getBean(SomeService.class);
        assertNotNull(result);
        assertEquals(mockBean, result);
    }

    @Test
    void setApplicationContext_ShouldOverwritePreviousContext_WhenCalledMultipleTimes() {
        // Arrange
        ApplicationContext firstContext = mock(ApplicationContext.class);
        ApplicationContext secondContext = mock(ApplicationContext.class);

        SomeService beanFromSecond = mock(SomeService.class);
        when(secondContext.getBean(SomeService.class)).thenReturn(beanFromSecond);

        // Act
        holder.setApplicationContext(firstContext);
        holder.setApplicationContext(secondContext); // ghi đè context cũ

        // Assert: context hiện tại phải là secondContext
        SomeService result = ApplicationContextHolder.getBean(SomeService.class);
        assertEquals(beanFromSecond, result);
        verify(firstContext, never()).getBean(SomeService.class);
    }

    @Test
    void getBean_ShouldReturnCorrectBean_WhenBeanExists() {
        // Arrange
        SomeService expectedBean = mock(SomeService.class);
        when(mockContext.getBean(SomeService.class)).thenReturn(expectedBean);
        holder.setApplicationContext(mockContext);

        // Act
        SomeService result = ApplicationContextHolder.getBean(SomeService.class);

        // Assert
        assertNotNull(result);
        assertEquals(expectedBean, result);
        verify(mockContext, times(1)).getBean(SomeService.class);
    }

    @Test
    void getBean_ShouldThrowException_WhenBeanNotFound() {
        // Arrange
        when(mockContext.getBean(SomeService.class))
                .thenThrow(new org.springframework.beans.factory.NoSuchBeanDefinitionException(SomeService.class));
        holder.setApplicationContext(mockContext);

        // Act & Assert
        assertThrows(
                org.springframework.beans.factory.NoSuchBeanDefinitionException.class,
                () -> ApplicationContextHolder.getBean(SomeService.class)
        );
    }

    @Test
    void getBean_ShouldThrowNullPointerException_WhenContextNotInitialized() {
        // Arrange: reset context về null bằng cách tạo holder mới mà không set context
        // (Lưu ý: field static nên cần đảm bảo thứ tự test hoặc dùng reflection)
        try {
            java.lang.reflect.Field field = ApplicationContextHolder.class.getDeclaredField("context");
            field.setAccessible(true);
            field.set(null, null); // reset context về null
        } catch (Exception e) {
            fail("Không thể reset context: " + e.getMessage());
        }

        // Act & Assert
        assertThrows(
                NullPointerException.class,
                () -> ApplicationContextHolder.getBean(SomeService.class)
        );
    }

    static class SomeService {}
}
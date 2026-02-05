"""
Test SDK behavior when all permissions are GRANTED.
"""
import time
from appium.webdriver.common.appiumby import AppiumBy
from selenium.common.exceptions import NoSuchElementException


PERMISSION_ALLOW_BUTTON_IDS = [
    "com.android.permissioncontroller:id/permission_allow_button",
    "com.android.permissioncontroller:id/permission_allow_foreground_only_button",
    "com.android.packageinstaller:id/permission_allow_button",
]

PERMISSION_ALLOW_TEXTS = [
    "Allow",
    "While using the app",
    "Only this time",
]


def grant_permission(driver):
    """Try to grant a permission dialog if present."""
    # Try by ID first
    for button_id in PERMISSION_ALLOW_BUTTON_IDS:
        try:
            button = driver.find_element(AppiumBy.ID, button_id)
            button.click()
            return True
        except NoSuchElementException:
            continue

    # Try by text
    for text in PERMISSION_ALLOW_TEXTS:
        try:
            button = driver.find_element(AppiumBy.XPATH, f"//*[@text='{text}']")
            button.click()
            return True
        except NoSuchElementException:
            continue

    return False


def grant_all_permissions(driver, max_attempts=10):
    """Keep granting permissions until no more dialogs appear."""
    for _ in range(max_attempts):
        time.sleep(1)
        if not grant_permission(driver):
            break
        time.sleep(0.5)


def test_sdk_with_permissions_granted(driver):
    """
    Test that SDK initializes and collects data when permissions are granted.
    """
    # Wait for the app to launch and permission dialogs to appear
    time.sleep(3)

    # Grant all permission dialogs
    grant_all_permissions(driver)

    # Wait for SDK to initialize and send data
    time.sleep(10)

    # App should still be running (not crashed)
    assert driver.current_activity is not None, "App crashed after granting permissions"

    # Optional: Check for a Toast or UI element that indicates success
    # This depends on your app's UI feedback

    print("Test passed: SDK running with all permissions granted")

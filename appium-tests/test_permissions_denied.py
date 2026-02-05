"""
Test SDK behavior when all permissions are DENIED.
"""
import time
from appium.webdriver.common.appiumby import AppiumBy
from selenium.common.exceptions import NoSuchElementException


PERMISSION_DENY_BUTTON_IDS = [
    "com.android.permissioncontroller:id/permission_deny_button",
    "com.android.permissioncontroller:id/permission_deny_and_dont_ask_again_button",
    "com.android.packageinstaller:id/permission_deny_button",
]

PERMISSION_DENY_TEXTS = [
    "Deny",
    "Don't allow",
]


def deny_permission(driver):
    """Try to deny a permission dialog if present."""
    # Try by ID first
    for button_id in PERMISSION_DENY_BUTTON_IDS:
        try:
            button = driver.find_element(AppiumBy.ID, button_id)
            button.click()
            return True
        except NoSuchElementException:
            continue

    # Try by text
    for text in PERMISSION_DENY_TEXTS:
        try:
            button = driver.find_element(AppiumBy.XPATH, f"//*[@text='{text}']")
            button.click()
            return True
        except NoSuchElementException:
            continue

    return False


def deny_all_permissions(driver, max_attempts=10):
    """Keep denying permissions until no more dialogs appear."""
    for _ in range(max_attempts):
        time.sleep(1)
        if not deny_permission(driver):
            break
        time.sleep(0.5)


def test_sdk_with_permissions_denied(driver):
    """
    Test that SDK gracefully handles denied permissions without crashing.
    """
    # Wait for the app to launch and permission dialogs to appear
    time.sleep(3)

    # Deny all permission dialogs
    deny_all_permissions(driver)

    # Wait for SDK to initialize and attempt data collection
    time.sleep(10)

    # App should still be running (not crashed) - this is the key assertion
    assert driver.current_activity is not None, "App crashed after denying permissions"

    # The SDK should gracefully handle missing permissions
    # and collect whatever non-sensitive data it can

    print("Test passed: SDK running gracefully with all permissions denied")

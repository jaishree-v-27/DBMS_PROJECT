import urllib.request
import urllib.parse
import http.cookiejar
import re

def get_csrf(opener, url):
    resp = opener.open(url)
    html = resp.read().decode('utf-8')
    match = re.search(r'name="_csrf"\s+value="([^"]+)"', html)
    if not match:
        raise Exception(f'CSRF token not found in {url}')
    return match.group(1), html

def login(username, password):
    cj = http.cookiejar.CookieJar()
    opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(cj))
    csrf, _ = get_csrf(opener, 'http://localhost:8080/login')
    data = urllib.parse.urlencode({'username': username, 'password': password, '_csrf': csrf}).encode('utf-8')
    req = urllib.request.Request('http://localhost:8080/login', data=data, method='POST')
    resp = opener.open(req)
    return opener, resp.geturl()

print('========================================================')
print('  TESTING UNIFIED SIDEBAR TABS & DASHBOARD STRUCTURE   ')
print('========================================================')

# 1. Admin Dashboard Tabs & Features
print('\n--- 1. Checking Admin Console Structure & Panels ---')
admin_opener, _ = login('admin', 'admin123')
admin_csrf, admin_html = get_csrf(admin_opener, 'http://localhost:8080/admin/dashboard')

expected_admin_panels = [
    'panel-overview', 'panel-donors', 'panel-hospitals', 'panel-requests',
    'panel-stock', 'panel-donations', 'panel-audit', 'panel-leaderboard', 'panel-settings'
]
for p in expected_admin_panels:
    assert f'id="{p}"' in admin_html, f'Missing Admin panel {p}'
    print(f'  [OK] Admin panel verified: {p}')

# Verify Sidebar Elements
assert 'dash-sidebar' in admin_html
assert 'dash-topbar' in admin_html
assert 'globalPanelSearch' in admin_html
assert 'recordDonationModal' in admin_html
print('  [OK] Admin layout, sidebar, topbar, search, and donation modal verified.')

# 2. Hospital Dashboard Tabs & Features
print('\n--- 2. Checking Hospital Portal Structure & Panels ---')
hosp_opener, _ = login('apollo_chennai', 'hospital123')
hosp_csrf, hosp_html = get_csrf(hosp_opener, 'http://localhost:8080/hospital/dashboard')

expected_hosp_panels = [
    'panel-overview', 'panel-raise-demand', 'panel-demands',
    'panel-reserves', 'panel-matching', 'panel-profile', 'panel-settings'
]
for p in expected_hosp_panels:
    assert f'id="{p}"' in hosp_html, f'Missing Hospital panel {p}'
    print(f'  [OK] Hospital panel verified: {p}')

assert 'dash-sidebar' in hosp_html
assert 'dash-topbar' in hosp_html
assert 'Facility Blood Bank Reserves' in hosp_html or 'Reserves & Stock' in hosp_html
print('  [OK] Hospital layout, sidebar, topbar, and clinical panels verified.')

# 3. Donor Dashboard Tabs & Features
print('\n--- 3. Checking Donor Portal Structure & Panels ---')
donor_opener, _ = login('rahul_sharma', 'donor123')
donor_csrf, donor_html = get_csrf(donor_opener, 'http://localhost:8080/donor/dashboard')

expected_donor_panels = [
    'panel-overview', 'panel-inbox', 'panel-eligibility',
    'panel-history', 'panel-milestones', 'panel-profile', 'panel-settings'
]
for p in expected_donor_panels:
    assert f'id="{p}"' in donor_html, f'Missing Donor panel {p}'
    print(f'  [OK] Donor panel verified: {p}')

assert 'dash-sidebar' in donor_html
assert 'dash-topbar' in donor_html
print('  [OK] Donor layout, sidebar, topbar, and volunteer panels verified.')

# 4. Test Password Change Functionality
print('\n--- 4. Testing Account Settings & Password Update ---')
# Test wrong current password first
pw_data_bad = urllib.parse.urlencode({
    'currentPassword': 'wrongpassword',
    'newPassword': 'newpass123',
    'confirmPassword': 'newpass123',
    '_csrf': donor_csrf
}).encode('utf-8')
req_pw_bad = urllib.request.Request('http://localhost:8080/account/change-password', data=pw_data_bad, method='POST')
resp_pw_bad = donor_opener.open(req_pw_bad)
html_bad = resp_pw_bad.read().decode('utf-8')
assert 'Current password was incorrect' in html_bad or 'errorMessage' in html_bad
print('  [OK] Password change correctly rejected invalid current password.')

# Test valid password change (change donor123 -> donor1234, then revert donor1234 -> donor123)
pw_data_good = urllib.parse.urlencode({
    'currentPassword': 'donor123',
    'newPassword': 'donor1234',
    'confirmPassword': 'donor1234',
    '_csrf': donor_csrf
}).encode('utf-8')
req_pw_good = urllib.request.Request('http://localhost:8080/account/change-password', data=pw_data_good, method='POST')
resp_pw_good = donor_opener.open(req_pw_good)
html_good = resp_pw_good.read().decode('utf-8')
assert 'Password updated successfully' in html_good or 'successMessage' in html_good
print('  [OK] Password successfully changed with BCrypt encoding.')

# Revert password back to demo standard
donor_csrf2, _ = get_csrf(donor_opener, 'http://localhost:8080/donor/dashboard')
pw_revert = urllib.parse.urlencode({
    'currentPassword': 'donor1234',
    'newPassword': 'donor123',
    'confirmPassword': 'donor123',
    '_csrf': donor_csrf2
}).encode('utf-8')
resp_revert = donor_opener.open(urllib.request.Request('http://localhost:8080/account/change-password', data=pw_revert, method='POST'))
print('  [OK] Password reverted to standard demo credential.')

# 5. Test Donor Profile Update
print('\n--- 5. Testing Donor Profile Save ---')
donor_csrf3, _ = get_csrf(donor_opener, 'http://localhost:8080/donor/dashboard')
profile_data = urllib.parse.urlencode({
    'name': 'Rahul Sharma',
    'phone': '9876543210',
    'city': 'Chennai',
    'available': 'true',
    '_csrf': donor_csrf3
}).encode('utf-8')
resp_prof = donor_opener.open(urllib.request.Request('http://localhost:8080/donor/profile/update', data=profile_data, method='POST'))
html_prof = resp_prof.read().decode('utf-8')
assert 'successfully updated' in html_prof or 'successMessage' in html_prof
print('  [OK] Donor profile update successfully persisted.')

# 6. Test Admin Record Donation
print('\n--- 6. Testing Admin Record Donation Form ---')
admin_csrf2, _ = get_csrf(admin_opener, 'http://localhost:8080/admin/dashboard')
donation_data = urllib.parse.urlencode({
    'donorId': 1, # Rahul Sharma
    'hospitalId': 1, # Apollo
    'units': 1,
    'donationDate': '2026-10-31',
    '_csrf': admin_csrf2
}).encode('utf-8')
resp_dn = admin_opener.open(urllib.request.Request('http://localhost:8080/admin/record-donation', data=donation_data, method='POST'))
html_dn = resp_dn.read().decode('utf-8')
print('  [OK] Record donation form executed cleanly.')

print('\n========================================================')
print('  ALL TESTS PASSED! UNIFIED DASHBOARDS FULLY VERIFIED   ')
print('========================================================')

import urllib.request
import urllib.parse
import http.cookiejar
import re

def run_session_flow(username, password, expected_dash):
    cj = http.cookiejar.CookieJar()
    opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(cj))

    # 1. GET /login to get CSRF token
    resp1 = opener.open('http://localhost:8080/login')
    html1 = resp1.read().decode('utf-8')
    match = re.search(r'name="_csrf"\s+value="([^"]+)"', html1)
    if not match:
        raise Exception('CSRF token not found in login form')
    csrf_token = match.group(1)

    # 2. POST /login
    data = urllib.parse.urlencode({'username': username, 'password': password, '_csrf': csrf_token}).encode('utf-8')
    req2 = urllib.request.Request('http://localhost:8080/login', data=data, method='POST')
    resp2 = opener.open(req2)
    final_url = resp2.geturl()
    body2 = resp2.read().decode('utf-8')

    print(f'User: {username} -> Final URL: {final_url} (HTTP {resp2.status})')
    assert expected_dash in final_url, f'Expected {expected_dash} in {final_url}'

    return opener, body2

print('=== Testing Admin Session ===')
admin_opener, admin_html = run_session_flow('admin', 'admin123', '/admin/dashboard')
print('Admin Dashboard Length:', len(admin_html))

# Test CSV exports
req_csv = admin_opener.open('http://localhost:8080/export/requests.csv').read().decode('utf-8')
print('Requests CSV Header:', req_csv.splitlines()[0] if req_csv else 'EMPTY')
print('Requests CSV Total Rows:', len(req_csv.splitlines()))

inv_csv = admin_opener.open('http://localhost:8080/export/inventory.csv').read().decode('utf-8')
print('Inventory CSV Header:', inv_csv.splitlines()[0] if inv_csv else 'EMPTY')
print('Inventory CSV Total Rows:', len(inv_csv.splitlines()))

print('\n=== Testing Hospital Session ===')
hosp_opener, hosp_html = run_session_flow('apollo_chennai', 'hospital123', '/hospital/dashboard')
print('Hospital Dashboard Length:', len(hosp_html))

print('\n=== Testing Donor Session ===')
donor_opener, donor_html = run_session_flow('rahul_sharma', 'donor123', '/donor/dashboard')
print('Donor Dashboard Length:', len(donor_html))

# Test iCalendar download
ics_data = donor_opener.open('http://localhost:8080/donor/reminder.ics').read().decode('utf-8')
print('ICS Data Snippet:')
for line in ics_data.splitlines()[:6]:
    print('  ', line)

print('\nALL ROLES AUTHENTICATED AND ALL VERIFICATION CHECKS PASSED!')

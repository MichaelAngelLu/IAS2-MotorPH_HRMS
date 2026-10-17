"""
migrate_credentials.py
One-time tool: LoginCredentials.csv -> Supabase Auth (+ profiles table)

SIP Control 3: passwords are hashed locally with BCrypt (cost 12) and only the
hash is sent to Supabase. Plaintext exists only in memory during this run.

Setup:
    pip install bcrypt requests
    set SUPABASE_URL=https://xxxx.supabase.co
    set SUPABASE_SERVICE_KEY=<service_role key>     (never commit this)
Run:
    python migrate_credentials.py LoginCredentials.csv
"""
import csv
import os
import sys

import bcrypt
import requests

SUPABASE_URL = os.environ["SUPABASE_URL"].rstrip("/")
SERVICE_KEY = os.environ["SUPABASE_SERVICE_KEY"]
EMAIL_DOMAIN = "motorph.local"
BCRYPT_COST = 12

SERVICE_KEY = SERVICE_KEY.strip().strip('"').strip("'")

if SERVICE_KEY.startswith("sb_publishable_"):
    sys.exit("That is the publishable (anon) key. Use the secret key (sb_secret_...) "
             "or the legacy service_role key.")

HEADERS = {"apikey": SERVICE_KEY, "Content-Type": "application/json"}
if not SERVICE_KEY.startswith("sb_secret_"):
    # Legacy service_role keys are JWTs and are also sent as a Bearer token.
    # New sb_secret_ keys are NOT JWTs: sending them as Bearer causes a 401.
    HEADERS["Authorization"] = f"Bearer {SERVICE_KEY}"

ROLE_MAP = {"admin": "admin", "user": "employee", "employee": "employee"}
STATUS_MAP = {"active": "Active", "inactive": "Inactive"}


def hash_password(plain: str) -> str:
    return bcrypt.hashpw(plain.encode("utf-8"), bcrypt.gensalt(BCRYPT_COST)).decode("utf-8")


HEADER_ALIASES = {
    "employeeid": "employee_id", "employeenumber": "employee_id",
    "employeeno": "employee_id", "empid": "employee_id", "id": "employee_id",
    "pass": "password", "pwd": "password",
}


def norm_header(h: str) -> str:
    key = "".join(ch for ch in (h or "").lower() if ch.isalnum())
    if key == "employeeid":
        return "employee_id"
    return HEADER_ALIASES.get(key, key)


def read_rows(path):
    """Yield (line_no, row_dict) with normalised headers and auto-detected delimiter."""
    with open(path, newline="", encoding="utf-8-sig") as f:
        sample = f.read(4096)
        f.seek(0)
        try:
            dialect = csv.Sniffer().sniff(sample, delimiters=",;\t|")
        except csv.Error:
            dialect = csv.excel
        reader = csv.reader(f, dialect)
        headers = [norm_header(h) for h in next(reader)]
        print(f"Detected delimiter {dialect.delimiter!r}, columns: {headers}")
        for line_no, values in enumerate(reader, start=2):
            if not any(v.strip() for v in values):
                continue
            yield line_no, dict(zip(headers, values))


def validate(row, line_no):
    """Return (clean_row, None) or (None, reason)."""
    if "employee_id" not in row:
        return None, f"no employee_id column found (columns seen: {list(row.keys())})"
    try:
        emp_id = int(str(row["employee_id"]).strip())
    except ValueError:
        return None, f"employee_id value {row['employee_id']!r} is not a number"

    password = (row.get("password") or "").strip()
    if len(password) < 8:
        return None, "password shorter than 8 characters"

    role = ROLE_MAP.get((row.get("role") or "").strip().lower())
    if not role:
        return None, f"unknown role '{row.get('role')}'"

    status = STATUS_MAP.get((row.get("status") or "").strip().lower())
    if not status:
        return None, f"unknown status '{row.get('status')}'"

    return {"employee_id": emp_id, "password": password, "role": role, "status": status}, None


def create_auth_user(emp_id: int, pw_hash: str):
    """Create the Auth user with a pre-hashed password. Returns the user's UUID."""
    body = {
        "email": f"{emp_id}@{EMAIL_DOMAIN}",
        "password_hash": pw_hash,        # hash only, never plaintext
        "email_confirm": True,
        "app_metadata": {"employee_id": emp_id},
    }
    r = requests.post(f"{SUPABASE_URL}/auth/v1/admin/users", headers=HEADERS, json=body, timeout=30)
    if r.status_code in (200, 201):
        return r.json()["id"]
    raise RuntimeError(f"auth create failed ({r.status_code}): {r.text[:200]}")


def insert_profile(user_id: str, c: dict):
    body = {
        "id": user_id,
        "employee_id": c["employee_id"],
        "role": c["role"],
        "status": c["status"],
        "must_change_password": True,
    }
    h = {**HEADERS, "Prefer": "resolution=merge-duplicates"}
    r = requests.post(f"{SUPABASE_URL}/rest/v1/profiles", headers=h, json=body, timeout=30)
    if r.status_code not in (200, 201, 204):
        raise RuntimeError(f"profile insert failed ({r.status_code}): {r.text[:200]}")


def main(path: str):
    ok, rejected = 0, []
    for line_no, row in read_rows(path):
        clean, reason = validate(row, line_no)
        if reason:
            rejected.append((line_no, reason))
            continue
        try:
            user_id = create_auth_user(clean["employee_id"], hash_password(clean["password"]))
            insert_profile(user_id, clean)
            ok += 1
            print(f"[OK]   employee {clean['employee_id']} -> {clean['role']}")
        except Exception as ex:                           # never print the password
            rejected.append((line_no, str(ex)))
            print(f"[FAIL] employee {clean['employee_id']}: {ex}")

    with open("rejected_credentials.txt", "w", encoding="utf-8") as out:
        for line_no, reason in rejected:
            out.write(f"line {line_no}: {reason}\n")   # no passwords written
    print(f"\nDone. Imported {ok}, rejected {len(rejected)} (see rejected_credentials.txt)")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        sys.exit("usage: python migrate_credentials.py LoginCredentials.csv")
    main(sys.argv[1])

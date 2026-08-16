"""Run every static check. Exit non-zero if any fails."""
import subprocess, sys, os
HERE=os.path.dirname(os.path.abspath(__file__))
CHECKS=[("Backend structure","check_backend.py"),
        ("POJO completeness","check_pojos.py"),
        ("Method calls","check_calls.py"),
        # Tests are checked separately because the three checks above scan
        # src/main only - and a test that no longer compiles fails the build
        # just as hard as a broken main source.
        ("Test sources","check_tests.py"),
        ("Seed data","check_seed.py"),
        ("Frontend imports","check_frontend.py")]
fail=0
for label,script in CHECKS:
    print("="*62); print(label); print("="*62)
    r=subprocess.run([sys.executable, os.path.join(HERE,script)])
    if r.returncode: fail+=1
    print()
print("="*62)
print("ALL CHECKS PASSED" if not fail else f"{fail} CHECK(S) FAILED")
sys.exit(1 if fail else 0)

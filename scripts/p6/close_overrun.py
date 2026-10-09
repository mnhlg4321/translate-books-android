#!/usr/bin/env python3
"""Close one pending overrun reservation in a P6 group ledger by appending a SETTLE_OVERRUN record (append-only; no line is edited)."""
import argparse
import json
import sys

import spend_ledger


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--ledger", required=True)
    parser.add_argument("--group-id", required=True)
    parser.add_argument("--maximum-usd", required=True)
    parser.add_argument("--call-id", required=True)
    parser.add_argument("--provider-cost-usd", required=True, help="cost reported by the provider for this call, exactly as recorded")
    parser.add_argument("--evidence", default="", help="where the provider cost is recorded (run id, file, line)")
    args = parser.parse_args(argv)
    result = spend_ledger.close_overrun(args.ledger, args.group_id, args.maximum_usd,
                                        args.call_id, args.provider_cost_usd, args.evidence)
    print(json.dumps(result, sort_keys=True, separators=(",", ":")))
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (OSError, ValueError, KeyError, TypeError) as error:
        print("P6 overrun close refused: " + str(error), file=sys.stderr)
        sys.exit(1)

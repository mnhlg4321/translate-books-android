#!/usr/bin/env python3
"""Validate a pulled P6 group ledger and optionally check capacity for the next fixture."""
import argparse
import json
import os
import sys

import spend_ledger


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--ledger", required=True)
    parser.add_argument("--group-id", required=True)
    parser.add_argument("--maximum-usd", required=True)
    parser.add_argument("--required-usd")
    parser.add_argument("--allow-empty", action="store_true")
    args = parser.parse_args()
    path = os.path.abspath(args.ledger)
    result = spend_ledger.inspect(path, args.group_id, args.maximum_usd, allow_empty=args.allow_empty)
    if args.required_usd is not None:
        result.update(spend_ledger.preflight(result, args.required_usd))
    print(json.dumps(result, sort_keys=True, separators=(",", ":")))
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except (OSError, ValueError, KeyError, TypeError) as error:
        print("P6 spend ledger check failed: " + str(error), file=sys.stderr)
        sys.exit(1)

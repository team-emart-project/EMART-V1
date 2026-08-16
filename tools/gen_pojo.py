"""
Generates a Java response DTO with matching field / getter / setter /
all-args-constructor / builder.

Why a generator instead of editing the file by hand: on this project the same
bug has now happened twice — a hand patch added a field and its getter but
missed the builder slot or a constructor parameter, and the mismatch only
showed up at runtime as "setX(...) is undefined". Emitting all five places
from one field list makes that class of bug impossible.

Usage: import build_pojo() and pass a spec; see gen_dtos.py.
"""
from textwrap import indent


def build_pojo(package, class_name, fields, imports=(), class_javadoc="",
               class_annotations=()):
    """
    fields: list of (java_type, name, javadoc_or_None)
    """
    out = [f"package {package};\n"]
    for imp in imports:
        out.append(f"import {imp};")
    if imports:
        out.append("")

    if class_javadoc:
        out.append(class_javadoc.rstrip())
    for ann in class_annotations:
        out.append(ann)
    out.append(f"public class {class_name} {{\n")

    # fields
    for typ, name, doc in fields:
        if doc:
            out.append(indent(doc.rstrip(), "    "))
        out.append(f"    private {typ} {name};")
        out.append("")

    # no-args ctor
    out.append(f"    public {class_name}() {{\n    }}\n")

    # all-args ctor
    params = ", ".join(f"{t} {n}" for t, n, _ in fields)
    sig = f"    public {class_name}({params}) {{"
    if len(sig) > 110:                       # wrap long signatures readably
        head = f"    public {class_name}("
        pad = " " * len(head)
        parts, line = [], head
        for i, (t, n, _) in enumerate(fields):
            piece = f"{t} {n}" + ("," if i < len(fields) - 1 else "")
            if len(line) + len(piece) > 108:
                parts.append(line.rstrip())
                line = pad
            line += piece + " "
        parts.append(line.rstrip() + ") {")
        out.extend(parts)
    else:
        out.append(sig)
    for _, n, _ in fields:
        out.append(f"        this.{n} = {n};")
    out.append("    }\n")

    # accessors
    for typ, name, _ in fields:
        cap = name[0].upper() + name[1:]
        prefix = "is" if typ == "boolean" else "get"
        out.append(f"    public {typ} {prefix}{cap}() {{ return {name}; }}")
        out.append(f"    public void set{cap}({typ} {name}) {{ this.{name} = {name}; }}\n")

    # builder
    out.append("    public static Builder builder() {")
    out.append("        return new Builder();")
    out.append("    }\n")
    out.append("    public static class Builder {\n")
    for typ, name, _ in fields:
        out.append(f"        private {typ} {name};")
    out.append("")
    for typ, name, _ in fields:
        out.append(f"        public Builder {name}({typ} {name}) {{ this.{name} = {name}; return this; }}")
    out.append("")
    out.append(f"        public {class_name} build() {{")
    args = ", ".join(n for _, n, _ in fields)
    line = f"            return new {class_name}("
    if len(line) + len(args) > 108:
        pad = " " * 20
        chunks, cur = [], line
        for i, (_, n, _) in enumerate(fields):
            piece = n + ("," if i < len(fields) - 1 else "")
            if len(cur) + len(piece) > 106:
                chunks.append(cur.rstrip())
                cur = pad
            cur += piece + " "
        chunks.append(cur.rstrip() + ");")
        out.extend(chunks)
    else:
        out.append(line + args + ");")
    out.append("        }")
    out.append("    }")
    out.append("}")
    return "\n".join(out) + "\n"

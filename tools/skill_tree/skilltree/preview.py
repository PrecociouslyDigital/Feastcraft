"""An SVG of the tree, to review a layout change without booting the game."""

from html import escape
from pathlib import Path

from .layout import Tree
from .model import Content, Keystone, Notable, Origin, Small, Start, Travel

COLOURS = ("#c23b4a", "#3f6fc0", "#b97a14", "#46882f", "#1b8898", "#c0558f", "#8a8520", "#7650c8")
NEUTRAL = "#8b849c"

RADIUS = {Start: 15.0, Travel: 6.5, Small: 9.0, Notable: 13.0, Keystone: 24.0, Origin: 24.0}


def radius(content: Content) -> float:
    return RADIUS[type(content)]


def svg(tree: Tree) -> str:
    out = [
        '<svg xmlns="http://www.w3.org/2000/svg" viewBox="-1100 -1100 2200 2200" width="2200" height="2200">',
        '<rect x="-1100" y="-1100" width="2200" height="2200" fill="#17141f"/>',
    ]
    for a, b in tree.edges:
        pa, pb = tree.nodes[a].at, tree.nodes[b].at
        out.append(f'<line x1="{pa.x:.1f}" y1="{pa.y:.1f}" x2="{pb.x:.1f}" y2="{pb.y:.1f}" stroke="#5b5372" stroke-width="3"/>')
    for node in tree.nodes.values():
        colour = NEUTRAL if node.region is None else COLOURS[node.region]
        x, y, r = node.at.x, node.at.y, radius(node.content)
        title = f"<title>{escape(node.id)}: {escape(node.content.title)}</title>"
        match node.content:
            case Keystone() | Origin():
                points = f"{x},{y - r} {x + r},{y} {x},{y + r} {x - r},{y}"
                ring = ' stroke="#ece8f5" stroke-width="5"' if isinstance(node.content, Origin) else ""
                out.append(f'<polygon points="{points}" fill="{colour}"{ring}>{title}</polygon>')
                out.append(
                    f'<text x="{x:.1f}" y="{y + r + 22:.1f}" fill="#ece8f5" font-size="20" '
                    f'text-anchor="middle" font-family="sans-serif">{escape(node.content.title)}</text>'
                )
            case Travel():
                out.append(f'<circle cx="{x:.1f}" cy="{y:.1f}" r="{r}" fill="#17141f" stroke="{colour}" stroke-width="3">{title}</circle>')
            case Notable():
                out.append(f'<circle cx="{x:.1f}" cy="{y:.1f}" r="{r}" fill="{colour}" stroke="#ece8f5" stroke-width="3">{title}</circle>')
            case _:
                out.append(f'<circle cx="{x:.1f}" cy="{y:.1f}" r="{r}" fill="{colour}">{title}</circle>')
    out.append("</svg>")
    return "\n".join(out) + "\n"


def write(tree: Tree, path: Path) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(svg(tree))

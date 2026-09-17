import Link from "next/link";
import { NAV_CONFIG, ROLE_ACCENT_CLASSES, isNavItemActive } from "@/components/shared/nav-config";
import { cn } from "@/lib/utils";
import type { Role } from "@/types/auth";

interface SidebarNavigationProps {
  role: Role;
  pathname: string;
  onNavigate?: () => void;
}

/** Role-aware primary navigation. It is deliberately presentational and receives navigation state. */
export function SidebarNavigation({ role, pathname, onNavigate }: SidebarNavigationProps) {
  const groups = NAV_CONFIG[role];
  const accent = ROLE_ACCENT_CLASSES[role];

  return (
    <nav aria-label="Asosiy menyu" className="flex-1 overflow-y-auto px-3 py-4">
      {groups.map((group, groupIndex) => (
        <div key={group.label ?? `group-${groupIndex}`}>
          {group.label ? (
            <p className="mt-4 mb-1 px-3 text-xs font-medium tracking-wide text-muted-foreground/70 uppercase first:mt-0">
              {group.label}
            </p>
          ) : null}
          <ul className="space-y-0.5">
            {group.items.map((item) => {
              const active = isNavItemActive(pathname, item.href);
              const Icon = item.icon;
              return (
                <li key={item.href}>
                  <Link
                    href={item.href}
                    aria-current={active ? "page" : undefined}
                    onClick={onNavigate}
                    className={cn(
                      "flex items-center gap-2.5 rounded-md border-l-2 border-transparent px-3 py-2 text-sm font-medium transition-colors",
                      active
                        ? cn(
                            accent.active,
                            accent.border,
                            "shadow-[0_2px_14px_-4px_var(--accent-role)]",
                          )
                        : "text-muted-foreground hover:bg-muted/60",
                    )}
                  >
                    <Icon className="size-4 shrink-0" strokeWidth={1.75} />
                    <span className="truncate">{item.label}</span>
                  </Link>
                </li>
              );
            })}
          </ul>
        </div>
      ))}
    </nav>
  );
}

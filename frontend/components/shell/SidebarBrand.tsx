import Image from "next/image";
import Link from "next/link";
import { Badge } from "@/components/ui/badge";
import { NAV_CONFIG, ROLE_ACCENT_CLASSES } from "@/components/shared/nav-config";
import { cn } from "@/lib/utils";
import type { Role } from "@/types/auth";
import { ROLE_LABEL } from "./role";

interface SidebarBrandProps {
  role: Role;
  compact?: boolean;
  onNavigate?: () => void;
}

/** Brand anchor shared by desktop navigation and the mobile navigation drawer. */
export function SidebarBrand({ role, compact, onNavigate }: SidebarBrandProps) {
  const accent = ROLE_ACCENT_CLASSES[role];
  const rootHref = NAV_CONFIG[role][0]?.items[0]?.href ?? "/dashboard";

  return (
    <Link
      href={rootHref}
      onClick={onNavigate}
      className={cn(
        "flex items-center gap-2.5 px-4",
        compact ? "" : "border-b border-sidebar-border py-4",
      )}
    >
      <Image
        src="/logo-mark.svg"
        alt=""
        width={32}
        height={29}
        className="shrink-0"
        priority
      />
      <span className="flex flex-col leading-tight">
        <span className="font-heading text-sm font-semibold text-sidebar-foreground">
          AcademiX AI
        </span>
        <Badge variant={accent.badge} className="mt-0.5 w-fit">
          {ROLE_LABEL[role]}
        </Badge>
      </span>
    </Link>
  );
}

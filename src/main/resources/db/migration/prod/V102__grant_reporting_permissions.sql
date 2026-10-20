-- The seeded profiles had no permissions, so every /api/reports/** call answered 403 (even for Administrator).
-- Administrator and Financial see and export all reports; Read-only only sees them.
INSERT INTO profile_permissions (profile_id, module, screen, action)
SELECT p.id, 'reporting', s.screen, a.action
FROM profiles p
CROSS JOIN (VALUES ('dashboard'), ('abc-curve'), ('dre'), ('stock-turnover'), ('commissions'), ('purchases-by-supplier'),
                   ('fiscal-books'), ('assessed-taxes')) AS s(screen)
CROSS JOIN (VALUES ('VIEW'), ('EXPORT')) AS a(action)
WHERE (p.name IN ('Administrator', 'Financial') OR (p.name = 'Read-only' AND a.action = 'VIEW'))
  AND NOT EXISTS (SELECT 1 FROM profile_permissions x
                  WHERE x.profile_id = p.id AND x.module = 'reporting' AND x.screen = s.screen AND x.action = a.action);

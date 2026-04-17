-- Allow customers to delete their own notifications from the client app.

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_policies
        WHERE schemaname = 'public'
          AND tablename = 'notifications'
          AND policyname = 'notifications_delete'
    ) THEN
        CREATE POLICY notifications_delete
        ON public.notifications
        FOR DELETE
        USING (
            user_id IN (
                SELECT users.id
                FROM public.users
                WHERE users.auth_id = (SELECT auth.uid())
            )
        );
    END IF;
END $$;

# Supabase Setup Guide - Food Ordering App MVP

## Prerequisites

- Supabase account (https://supabase.com)
- Git (for version control)

---

## Step 1: Create Supabase Project

1. Go to https://supabase.com and sign in
2. Click **New Project**
3. Fill in project details:
   - **Name**: `food-ordering-app` (or your preferred name)
   - **Database Password**: Generate a strong password and save it!
   - **Region**: Select closest region (e.g., `Southeast Asia (Singapore)`)
   - **Pricing Plan**: Free tier is sufficient for MVP
4. Click **Create new project**
5. Wait for project to provision (1-2 minutes)

---

## Step 2: Get Project Credentials

1. Go to **Project Settings** (gear icon) → **API**
2. Copy and save these values:

```env
# Supabase Credentials
SUPABASE_URL=https://your-project-ref.supabase.co
SUPABASE_ANON_KEY=your-anon-key
SUPABASE_SERVICE_ROLE_KEY=your-service-role-key
```

**Important**: Keep `SUPABASE_SERVICE_ROLE_KEY` secret - never expose in app!

---

## Step 3: Run Database Migrations

1. Go to **SQL Editor** in left sidebar
2. Click **New query**
3. Copy content from `supabase/migrations/001_initial_schema.sql`
4. Paste into SQL Editor
5. Click **Run** (or press Cmd/Ctrl + Enter)
6. Repeat for remaining files in order:
   - `002_rpc_functions.sql`
   - `003_rls_policies.sql`
   - `004_seed_data.sql`

---

## Step 4: Configure Storage Buckets

### 4.1 Create Buckets

1. Go to **Storage** in left sidebar
2. Click **New bucket**
3. Create these buckets:

| Bucket Name | Public | File Size Limit |
|-------------|--------|-----------------|
| `avatars` | ✅ | 2MB |
| `restaurants` | ✅ | 5MB |
| `menu-items` | ✅ | 5MB |
| `banners` | ✅ | 10MB |

### 4.2 Set Storage Policies

For each bucket, create policies:

**avatars bucket:**
```sql
-- Allow public read
CREATE POLICY "Public avatars read" ON storage.objects
FOR SELECT USING (bucket_id = 'avatars');

-- Allow authenticated users to upload
CREATE POLICY "Auth upload avatars" ON storage.objects
FOR INSERT WITH CHECK (bucket_id = 'avatars' AND auth.role() = 'authenticated');
```

**restaurants bucket:**
```sql
CREATE POLICY "Public restaurants read" ON storage.objects
FOR SELECT USING (bucket_id = 'restaurants');

CREATE POLICY "Admin upload restaurants" ON storage.objects
FOR INSERT WITH CHECK (bucket_id = 'restaurants');
```

Repeat similar policies for `menu-items` and `banners`.

---

## Step 5: Configure Authentication

### 5.1 Enable Auth Providers

1. Go to **Authentication** → **Providers**
2. Enable **Email** (enabled by default)
3. Optional: Enable **Google** or **Facebook** for OAuth

### 5.2 Configure Auth Settings

1. Go to **Authentication** → **Settings**
2. Set **Site URL**: `http://localhost` (for development)
3. Set **Redirect URLs**:
   ```
   myapp://
   http://localhost
   ```

---

## Step 6: Environment Variables

Create `.env` file in your Android projects:

```env
# Supabase - Client App
SUPABASE_URL=https://your-project-ref.supabase.co
SUPABASE_ANON_KEY=your-anon-key

# Supabase - Admin App  
SUPABASE_URL=https://your-project-ref.supabase.co
SUPABASE_ANON_KEY=your-anon-key
```

**Note**: Never commit `.env` to git! Add to `.gitignore`:

```
.env
.env.*
```

---

## Step 7: Test Setup

### 7.1 Create Test Users

1. Go to **Authentication** → **Users**
2. Click **Invite user** to create admin user
3. Set email and temporary password

### 7.2 Link Auth to Users Table

After creating auth user, link to `users` table:

```sql
-- Update user role to admin (use your auth user email)
UPDATE users 
SET role = 'admin', auth_id = (SELECT id FROM auth.users WHERE email = 'admin@yourapp.com')
WHERE email = 'admin@yourapp.com';
```

### 7.3 Test RPC Functions

Test order creation:

```sql
SELECT rpc_create_order(
    p_user_id := (SELECT id FROM users LIMIT 1),
    p_restaurant_id := (SELECT id FROM restaurants LIMIT 1),
    p_delivery_address := '123 Test St, District 1, HCM',
    p_delivery_latitude := 10.7769,
    p_delivery_longitude := 106.7000,
    p_note := 'Test order',
    p_payment_method := 'cod',
    p_promotion_code := NULL,
    p_items := '[{"menu_item_id": "xxx", "quantity": 1}]'::jsonb
);
```

---

## Step 8: Project Structure for Development

### Environment Strategy

| Environment | Use For | Supabase Project |
|-------------|---------|------------------|
| DEV | Local development | Development project |
| STAGING | QA testing | Staging project |
| PROD | Production | Production project |

### Recommended Setup

1. **One Supabase project per environment**
2. Use migration files for version control
3. Seed different data per environment

---

## Step 9: Backup & Recovery

### 9.1 Enable Point-in-Time Recovery (Optional)

Go to **Database** → **Backups** → Enable if on paid plan

### 9.2 Manual Backup

```sql
-- Export all data
SELECT * FROM users;
SELECT * FROM restaurants;
SELECT * FROM orders;
-- etc.
```

---

## Troubleshooting

### Common Issues

| Issue | Solution |
|-------|----------|
| RLS blocked access | Check policies in SQL Editor |
| Auth not working | Verify Site URL in Auth Settings |
| Storage upload fails | Check bucket policies |
| RPC function errors | Check function permissions |

### Reset Database

If needed, reset database:

1. Go to **Database** → **Reset database**
2. This will drop all tables - re-run migrations

---

## Next Steps

After setup:

1. [ ] Configure Android projects to connect to Supabase
2. [ ] Test authentication flow
3. [ ] Verify RLS policies
4. [ ] Test RPC functions
5. [ ] Start implementing Phase 1

---

## Resources

- [Supabase Docs](https://supabase.com/docs)
- [PostgreSQL Docs](https://www.postgresql.org/docs/)
- [Auth Guide](https://supabase.com/docs/guides/auth)
- [Storage Guide](https://supabase.com/docs/guides/storage)
- [RLS Guide](https://supabase.com/docs/guides/auth/row-level-security)
